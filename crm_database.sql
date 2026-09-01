-- =========================================================
-- DATABASE: telephony_crm
-- DESCRIPTION: Master Schema for Omnichannel Telephony CRM
-- =========================================================

-- 1. EXTENSIONS
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 2. ENUMS & TYPES
CREATE TYPE lead_source AS ENUM ('META', 'GOOGLE', 'DIRECT', 'WEBSITE', 'REFERRAL');
CREATE TYPE intent_level AS ENUM ('HOT', 'WARM', 'COLD', 'SPAM', 'DUPLICATE');
CREATE TYPE deal_status AS ENUM ('NEW', 'IN_PROGRESS', 'CONTACTED', 'QUALIFIED', 'PROPOSAL_SENT', 'CLOSED_WON', 'CLOSED_LOST');
CREATE TYPE sentiment_type AS ENUM ('POSITIVE', 'NEUTRAL', 'NEGATIVE');

-- 3. USERS & AGENTS TABLE (Telecallers & Admins)
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) DEFAULT 'TELECALLER', -- ADMIN, MANAGER, TELECALLER
    extension VARCHAR(20) UNIQUE,          -- e.g., '1001'
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 4. MASTER LEADS TABLE
CREATE TABLE IF NOT EXISTS leads (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(200) NOT NULL,
    phone_number VARCHAR(30) NOT NULL,
    email VARCHAR(150),
    source VARCHAR(50) DEFAULT 'DIRECT',
    vertical VARCHAR(50) DEFAULT 'GENERAL', -- REAL_ESTATE, HEALTHCARE, EDTECH, BFSI
    campaign_name VARCHAR(150),
    city VARCHAR(100),
    assigned_agent_id BIGINT REFERENCES users(id) ON DELETE SET NULL,

    -- Raw Payload & Metadata
    raw_lead_data TEXT,

    -- AI Generated Fields
    ai_lead_score INTEGER CHECK (ai_lead_score BETWEEN 0 AND 100),
    intent_category VARCHAR(20) DEFAULT 'COLD',
    ai_agent_advice TEXT,

    -- Pipeline & Financials
    status VARCHAR(50) DEFAULT 'NEW',
    deal_value NUMERIC(15, 2) DEFAULT 0.00,

    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indexing for quick filtering on the UI
CREATE INDEX IF NOT EXISTS idx_leads_phone ON leads(phone_number);
CREATE INDEX IF NOT EXISTS idx_leads_source ON leads(source);
CREATE INDEX IF NOT EXISTS idx_leads_intent ON leads(intent_category);
CREATE INDEX IF NOT EXISTS idx_leads_vertical ON leads(vertical);

-- 5. CALL LOGS & TELEPHONY ENGINE TABLE
CREATE TABLE IF NOT EXISTS call_logs (
    id BIGSERIAL PRIMARY KEY,
    channel_id VARCHAR(100) UNIQUE NOT NULL,
    caller_number VARCHAR(50) NOT NULL,
    agent_extension VARCHAR(20),
    direction VARCHAR(20) DEFAULT 'INBOUND', -- INBOUND, OUTBOUND
    duration_seconds INTEGER DEFAULT 0,
    recording_file_name VARCHAR(255),
    recording_url TEXT,
    call_status VARCHAR(50) DEFAULT 'ANSWERED', -- ANSWERED, NO_ANSWER, BUSY, FAILED

    initiated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_calls_caller ON call_logs(caller_number);
CREATE INDEX IF NOT EXISTS idx_calls_channel ON call_logs(channel_id);

-- 6. VOICE AI & AUDIT ENGINE
CREATE TABLE IF NOT EXISTS voice_ai_audits (
    id BIGSERIAL PRIMARY KEY,
    call_log_id BIGINT REFERENCES call_logs(id) ON DELETE CASCADE,
    channel_id VARCHAR(100) NOT NULL,
    agent_name VARCHAR(100),
    customer_phone VARCHAR(50),

    transcript TEXT,
    summary TEXT,
    sentiment VARCHAR(20) DEFAULT 'NEUTRAL',
    quality_score INTEGER CHECK (quality_score BETWEEN 0 AND 100),
    objections_detected TEXT,

    audited_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 7. VISUAL IVR FLOWS & QUEUES
CREATE TABLE IF NOT EXISTS ivr_flows (
    id BIGSERIAL PRIMARY KEY,
    flow_name VARCHAR(100) NOT NULL,
    dnis_number VARCHAR(50) UNIQUE NOT NULL, -- Incoming DID mapping
    flow_json TEXT NOT NULL,                 -- Node routing graph (JSON)
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 8. BULK WHATSAPP CAMPAIGNS & MESSAGE LOGS
CREATE TABLE IF NOT EXISTS whatsapp_campaigns (
    id BIGSERIAL PRIMARY KEY,
    campaign_name VARCHAR(150) NOT NULL,
    template_name VARCHAR(100) NOT NULL,
    language_code VARCHAR(10) DEFAULT 'en_US',
    total_recipients INTEGER DEFAULT 0,
    sent_count INTEGER DEFAULT 0,
    failed_count INTEGER DEFAULT 0,
    status VARCHAR(50) DEFAULT 'COMPLETED',
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS whatsapp_messages (
    id BIGSERIAL PRIMARY KEY,
    campaign_id BIGINT REFERENCES whatsapp_campaigns(id) ON DELETE SET NULL,
    recipient_phone VARCHAR(30) NOT NULL,
    template_name VARCHAR(100),
    status VARCHAR(50) DEFAULT 'SENT', -- SENT, DELIVERED, READ, FAILED
    error_message TEXT,
    sent_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 9. TELECALLER SCRATCHPAD NOTES & CALLBACKS
CREATE TABLE IF NOT EXISTS telecaller_notes (
    id BIGSERIAL PRIMARY KEY,
    lead_id BIGINT REFERENCES leads(id) ON DELETE CASCADE,
    agent_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    note_content TEXT NOT NULL,
    callback_time TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- =========================================================
-- INITIAL SEED DATA (Ready for Testing)
-- =========================================================

-- Seed User Agent
INSERT INTO users (username, email, password_hash, role, extension)
VALUES ('Agent 1001', 'agent1001@crm.local', '$2a$10$7EqJtq98hPqEX7fNZaFWoO', 'TELECALLER', '1001')
ON CONFLICT (username) DO NOTHING;

-- Seed Sample Leads Across Verticals (Fixed: ai_agent_advice)
INSERT INTO leads (full_name, phone_number, email, source, vertical, raw_lead_data, ai_lead_score, intent_category, ai_agent_advice, status, deal_value)
VALUES
('Vikram Malhotra', '+919876543210', 'vikram@example.com', 'META', 'REAL_ESTATE', 'Looking for 3BHK Villa in Bangalore, Budget 1.5 Cr', 92, 'HOT', 'Ask for preferred possession timeline and loan pre-approval.', 'NEW', 15000000.00),
('Ananya Deshmukh', '+919876543211', 'ananya@medtech.com', 'GOOGLE', 'HEALTHCARE', 'Hospital Management System for 50-bed facility', 88, 'HOT', 'Check if they need custom HL7/EMR integrations.', 'NEW', 450000.00),
('Rahul Sharma', '+919876543212', 'rahul@gmail.com', 'WEBSITE', 'EDTECH', 'Inquiry for Full Stack Java Bootcamp', 55, 'WARM', 'Offer the weekend batch schedule and EMI options.', 'IN_PROGRESS', 35000.00),
('Pooja Mehta', '+919876543213', 'pooja@finserve.in', 'DIRECT', 'BFSI', 'Personal Loan Inquiry', 20, 'COLD', 'Verify CIBIL score and current employment.', 'NEW', 100000.00);

-- Seed Default IVR Flow
INSERT INTO ivr_flows (flow_name, dnis_number, flow_json, is_active)
VALUES (
    'Main Company IVR',
    '1800123456',
    '[{"step":1,"action":"PLAY_PROMPT","media":"sound:welcome"},{"step":2,"action":"DTMF_MENU","options":{"1":"QUEUE_SALES","2":"QUEUE_SUPPORT"}},{"step":3,"action":"DIAL_EXTENSION","target":"1001"}]',
    TRUE
) ON CONFLICT (dnis_number) DO NOTHING;