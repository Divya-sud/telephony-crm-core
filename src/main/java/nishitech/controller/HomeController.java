package nishitech.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public String index() {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Universal Telephony CRM & AI Engine</title>
                <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
                <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
                <style>
                    :root {
                        --sidebar-bg: #0f172a;
                        --body-bg: #020617;
                        --card-bg: #1e293b;
                        --border: #334155;
                        --primary: #38bdf8;
                        --text: #f8fafc;
                        --text-muted: #94a3b8;
                    }
                    body { background: var(--body-bg); color: var(--text); font-family: 'Segoe UI', system-ui, sans-serif; margin: 0; display: flex; height: 100vh; overflow: hidden; }
                    
                    /* Sidebar Styling */
                    .sidebar { width: 280px; background: var(--sidebar-bg); border-right: 1px solid var(--border); overflow-y: auto; padding: 20px 14px; flex-shrink: 0; }
                    .sidebar .logo { font-size: 20px; font-weight: bold; color: #fff; margin-bottom: 24px; padding-left: 10px; display: flex; align-items: center; }
                    .sidebar .menu-title { font-size: 11px; text-transform: uppercase; letter-spacing: 1px; color: var(--text-muted); margin: 18px 0 6px 10px; font-weight: 700; }
                    .sidebar a { display: flex; align-items: center; justify-content: space-between; padding: 10px 12px; color: var(--text-muted); text-decoration: none; border-radius: 6px; font-size: 14px; transition: 0.2s; margin-bottom: 2px; }
                    .sidebar a:hover, .sidebar a.active { background: rgba(56, 189, 248, 0.1); color: var(--primary); }
                    .sidebar .submenu { padding-left: 16px; }
                    .sidebar .submenu a { font-size: 13px; padding: 7px 12px; }
                    
                    /* Main Content Area */
                    .main-wrapper { flex-grow: 1; display: flex; flex-direction: column; overflow-y: auto; background: var(--body-bg); }
                    .topbar { height: 65px; border-bottom: 1px solid var(--border); background: var(--sidebar-bg); display: flex; align-items: center; justify-content: space-between; padding: 0 28px; flex-shrink: 0; }
                    .content-area { padding: 28px; flex-grow: 1; }
                    
                    .card { background: var(--card-bg); border: 1px solid var(--border); border-radius: 10px; color: var(--text); margin-bottom: 24px; }
                    .card-header { background: rgba(255,255,255,0.02); border-bottom: 1px solid var(--border); font-weight: 600; padding: 16px 20px; }
                    
                    /* Table Styling */
                    table { width: 100%; border-collapse: collapse; }
                    th { background: #0b1120; color: var(--text-muted); padding: 12px 16px; font-size: 12px; text-transform: uppercase; border-bottom: 1px solid var(--border); }
                    td { padding: 14px 16px; border-bottom: 1px solid var(--border); font-size: 14px; }
                    
                    .badge-hot { background: rgba(239, 68, 68, 0.2); color: #f87171; border: 1px solid #ef4444; }
                    .badge-warm { background: rgba(245, 158, 11, 0.2); color: #fbbf24; border: 1px solid #f59e0b; }
                    .badge-cold { background: rgba(56, 189, 248, 0.2); color: #38bdf8; border: 1px solid #38bdf8; }
                    
                    /* Form Controls */
                    input, select, textarea { background: #0b1120 !important; border: 1px solid var(--border) !important; color: #fff !important; }
                    input:focus, select:focus, textarea:focus { border-color: var(--primary) !important; box-shadow: none !important; }
                </style>
            </head>
            <body>

                <!-- SIDEBAR -->
                <div class="sidebar">
                    <div class="logo">
                        <i class="fa fa-chart-line text-primary me-2"></i> Universal CRM
                    </div>

                    <div class="menu-title">Main Dashboards</div>
                    <a href="#" class="active" onclick="switchView('dashboard')"><span><i class="fa fa-chart-line me-2"></i> Dashboard</span></a>
                    <a href="#" onclick="switchView('realtime')"><span><i class="fa fa-bolt me-2"></i> Realtime Engine</span></a>

                    <div class="menu-title">Lead Management</div>
                    <a data-bs-toggle="collapse" href="#leadMenu" role="button">
                        <span><i class="fa fa-users me-2"></i> Pipeline Leads</span> <i class="fa fa-angle-down"></i>
                    </a>
                    <div class="collapse show submenu" id="leadMenu">
                        <a href="#" onclick="loadLeadsFiltered(null)">All Inquiries</a>
                        <a href="#" onclick="loadLeadsFiltered('HOT')"><i class="fa fa-fire text-danger me-1"></i> Hot Prospects</a>
                        <a href="#" onclick="loadLeadsFiltered('WARM')"><i class="fa fa-sun text-warning me-1"></i> Warm Prospects</a>
                        <a href="#" onclick="loadLeadsFiltered('COLD')"><i class="fa fa-snowflake text-info me-1"></i> Cold Prospects</a>
                    </div>

                    <div class="menu-title">Business Sectors</div>
                    <a data-bs-toggle="collapse" href="#sectorMenu" role="button">
                        <span><i class="fa fa-briefcase me-2"></i> Vertical Views</span> <i class="fa fa-angle-down"></i>
                    </a>
                    <div class="collapse submenu" id="sectorMenu">
                        <a href="#" onclick="loadVertical('REAL_ESTATE')"><i class="fa fa-building me-2"></i> Real Estate</a>
                        <a href="#" onclick="loadVertical('HEALTHCARE')"><i class="fa fa-hospital me-2"></i> Healthcare</a>
                        <a href="#" onclick="loadVertical('EDTECH')"><i class="fa fa-graduation-cap me-2"></i> EdTech</a>
                        <a href="#" onclick="loadVertical('BFSI')"><i class="fa fa-coins me-2"></i> BFSI / Finance</a>
                    </div>

                    <div class="menu-title">Cloud Telephony & IVR</div>
                    <a href="#" onclick="switchView('telephony')"><span><i class="fa fa-phone me-2"></i> Web Dialer & Calls</span></a>
                    <a href="#" onclick="switchView('ivr')"><span><i class="fa fa-headphones me-2"></i> IVR Builder & Queues</span></a>

                    <div class="menu-title">AI Intelligence</div>
                    <a href="#" onclick="switchView('copilot')"><span><i class="fa fa-robot me-2"></i> Live Agent Copilot</span></a>
                    <a href="#" onclick="switchView('ai-audits')"><span><i class="fa fa-brain me-2"></i> Voice Quality Audits</span></a>

                    <div class="menu-title">Marketing & Ads</div>
                    <a href="#" onclick="switchView('whatsapp')"><span><i class="fab fa-whatsapp me-2 text-success"></i> WhatsApp Campaigns</span></a>
                    <a href="#" onclick="switchView('ads')"><span><i class="fab fa-meta me-2"></i> Meta & Google Ads</span></a>
                </div>

                <!-- MAIN DISPLAY AREA -->
                <div class="main-wrapper">
                    <!-- TOPBAR -->
                    <div class="topbar">
                        <div style="font-weight: 600; font-size: 16px;" id="currentViewTitle">Operations Dashboard</div>
                        <div class="d-flex align-items-center gap-3">
                            <span class="badge bg-success"><i class="fa fa-circle me-1" style="font-size: 8px;"></i> Asterisk Online: 5065</span>
                            <span class="badge bg-primary"><i class="fa fa-microchip me-1"></i> Ollama AI Active</span>
                            <button class="btn btn-sm btn-outline-info" onclick="openNewLeadModal()"><i class="fa fa-plus me-1"></i> Quick Lead</button>
                        </div>
                    </div>

                    <!-- CONTENT CONTAINER -->
                    <div class="content-area" id="mainContent">
                        <!-- Dynamic View Components Loaded via JS -->
                    </div>
                </div>

                <!-- LEAD CREATION MODAL -->
                <div class="modal fade" id="newLeadModal" tabindex="-1">
                    <div class="modal-dialog">
                        <div class="modal-content bg-dark border-secondary text-white">
                            <div class="modal-header border-secondary">
                                <h5 class="modal-title">Ingest Lead to AI Pipeline</h5>
                                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                            </div>
                            <div class="modal-body">
                                <input type="text" id="mName" class="form-control mb-2" placeholder="Full Name">
                                <input type="text" id="mPhone" class="form-control mb-2" placeholder="Phone (e.g. +919876543210)">
                                <select id="mVertical" class="form-select mb-2">
                                    <option value="REAL_ESTATE">Real Estate</option>
                                    <option value="HEALTHCARE">Healthcare</option>
                                    <option value="EDTECH">EdTech</option>
                                    <option value="BFSI">BFSI / Banking</option>
                                </select>
                                <select id="mSource" class="form-select mb-2">
                                    <option value="META">Meta Ads</option>
                                    <option value="GOOGLE">Google Search</option>
                                    <option value="DIRECT">Direct Inbound</option>
                                </select>
                                <textarea id="mNotes" class="form-control mb-2" rows="3" placeholder="Lead Requirement / Context notes..."></textarea>
                            </div>
                            <div class="modal-footer border-secondary">
                                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
                                <button type="button" class="btn btn-primary" onclick="submitLead()">Ingest & Score</button>
                            </div>
                        </div>
                    </div>
                </div>

                <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
                <script>
                    const modal = new bootstrap.Modal(document.getElementById('newLeadModal'));

                    function openNewLeadModal() { modal.show(); }

                    async function submitLead() {
                        const payload = {
                            fullName: document.getElementById('mName').value,
                            phoneNumber: document.getElementById('mPhone').value,
                            vertical: document.getElementById('mVertical').value,
                            source: document.getElementById('mSource').value,
                            rawLeadData: document.getElementById('mNotes').value
                        };
                        await fetch('/api/v1/leads/create', {
                            method: 'POST',
                            headers: {'Content-Type': 'application/json'},
                            body: JSON.stringify(payload)
                        });
                        modal.hide();
                        switchView('dashboard');
                    }

                    // --- VIEW SWITCHER ---
                    function switchView(viewName) {
                        const content = document.getElementById('mainContent');
                        const title = document.getElementById('currentViewTitle');

                        if (viewName === 'dashboard') {
                            title.innerText = "Executive Summary & Lead Pipeline";
                            content.innerHTML = `
                                <div class="row mb-4" id="statsRow">
                                    <div class="col-md-3"><div class="card p-3"><h5>Total Pipeline</h5><h2 class="text-primary" id="sTotal">-</h2></div></div>
                                    <div class="col-md-3"><div class="card p-3"><h5>Hot Leads</h5><h2 class="text-danger" id="sHot">-</h2></div></div>
                                    <div class="col-md-3"><div class="card p-3"><h5>Warm Leads</h5><h2 class="text-warning" id="sWarm">-</h2></div></div>
                                    <div class="col-md-3"><div class="card p-3"><h5>Calls Recorded</h5><h2 class="text-success" id="sCalls">-</h2></div></div>
                                </div>
                                <div class="card">
                                    <div class="card-header d-flex justify-content-between align-items-center">
                                        <span>Master Pipeline Inquiries</span>
                                        <button class="btn btn-sm btn-primary" onclick="loadLeadsTable()"><i class="fa fa-arrows-rotate"></i></button>
                                    </div>
                                    <div class="table-responsive">
                                        <table>
                                            <thead><tr><th>Name</th><th>Phone</th><th>Vertical</th><th>Source</th><th>AI Score</th><th>Intent</th><th>Actions</th></tr></thead>
                                            <tbody id="leadsTbody"></tbody>
                                        </table>
                                    </div>
                                </div>
                            `;
                            loadSummaryStats();
                            loadLeadsTable();
                        } else if (viewName === 'telephony') {
                            title.innerText = "Cloud Telephony & Live Agent Dialer";
                            content.innerHTML = `
                                <div class="row">
                                    <div class="col-md-4">
                                        <div class="card p-4 text-center">
                                            <h4>Agent Softphone (Ext: 1001)</h4>
                                            <input type="text" id="dialInput" class="form-control form-control-lg text-center my-3" placeholder="Enter Phone Number">
                                            <button class="btn btn-success btn-lg w-100" onclick="originateCallDirect()"><i class="fa fa-phone me-2"></i> Originate Call</button>
                                        </div>
                                    </div>
                                    <div class="col-md-8">
                                        <div class="card">
                                            <div class="card-header">Live PBX Channels & Audio Records</div>
                                            <table>
                                                <thead><tr><th>Channel</th><th>Caller</th><th>Audio File</th><th>Time</th></tr></thead>
                                                <tbody id="callsTbody"></tbody>
                                            </table>
                                        </div>
                                    </div>
                                </div>
                            `;
                            loadCallsTable();
                        } else if (viewName === 'copilot') {
                            title.innerText = "Real-Time Telecaller AI Copilot";
                            content.innerHTML = `
                                <div class="row">
                                    <div class="col-md-5">
                                        <div class="card p-4">
                                            <h5>Lead Interaction Context</h5>
                                            <textarea id="cpContext" class="form-control my-2" rows="3">Lead: High-value enterprise account, Budget 10k USD, Timeline 15 days.</textarea>
                                            <h5 class="mt-3">Customer Live Objection:</h5>
                                            <input type="text" id="cpStatement" class="form-control my-2" placeholder="e.g. Your competitors give 24/7 support for free.">
                                            <button class="btn btn-primary w-100 mt-2" onclick="runCopilot()"><i class="fa fa-wand-magic-sparkles me-1"></i> Generate Rebuttal</button>
                                        </div>
                                    </div>
                                    <div class="col-md-7">
                                        <div class="card p-4" id="cpOutput">
                                            <h5 class="text-info">Awaiting Input...</h5>
                                            <p class="text-muted">Type an objection on the left to receive immediate real-time AI scripts.</p>
                                        </div>
                                    </div>
                                </div>
                            `;
                        } else if (viewName === 'whatsapp') {
                            title.innerText = "WhatsApp Bulk Campaign Engine";
                            content.innerHTML = `
                                <div class="card p-4">
                                    <h4>Meta WhatsApp Official Broadcast</h4>
                                    <div class="mb-3">
                                        <label class="form-label">Phone Numbers (Comma separated):</label>
                                        <textarea id="waRecipients" class="form-control" rows="4" placeholder="+919876543210, +919876543211"></textarea>
                                    </div>
                                    <div class="row mb-3">
                                        <div class="col-md-6">
                                            <label class="form-label">Meta Approved Template Name:</label>
                                            <input type="text" id="waTemplate" class="form-control" value="lead_followup_discount">
                                        </div>
                                        <div class="col-md-6">
                                            <label class="form-label">Language Code:</label>
                                            <input type="text" id="waLang" class="form-control" value="en_US">
                                        </div>
                                    </div>
                                    <button class="btn btn-success" onclick="dispatchWhatsApp()"><i class="fab fa-whatsapp me-2"></i> Send Bulk Campaign</button>
                                    <div id="waResult" class="mt-3"></div>
                                </div>
                            `;
                        } else if (viewName === 'ivr') {
                            title.innerText = "Visual IVR Flow Builder";
                            content.innerHTML = `
                                <div class="card p-4">
                                    <h4>Inbound Call Flow Configuration</h4>
                                    <p class="text-muted">Configure your Asterisk Stasis IVR routing tree:</p>
                                    <div class="p-3 border border-secondary rounded mb-3">
                                        <b>Step 1:</b> Play Greeting Audio <code>sound:welcome-prompt</code><br>
                                        <b>Step 2:</b> Capture DTMF Tone (1: Sales Queue, 2: Support Queue)<br>
                                        <b>Step 3:</b> Bridge to Agent SIP Trunk <code>PJSIP/1001</code>
                                    </div>
                                    <button class="btn btn-outline-info" onclick="alert('IVR configuration synced with Asterisk dialplan extensions.conf!')"><i class="fa fa-sync me-2"></i> Save & Deploy Flow</button>
                                </div>
                            `;
                        }
                    }

                    // --- DATA LOADERS ---
                    async function loadSummaryStats() {
                        const res = await fetch('/api/v1/analytics/summary');
                        const data = await res.json();
                        document.getElementById('sTotal').innerText = data.totalLeads;
                        document.getElementById('sHot').innerText = data.hotLeads;
                        document.getElementById('sWarm').innerText = data.warmLeads;
                        document.getElementById('sCalls').innerText = data.totalCalls;
                    }

                    async function loadLeadsTable(filterUrl = '/api/v1/leads') {
                        const res = await fetch(filterUrl);
                        const leads = await res.json();
                        const tbody = document.getElementById('leadsTbody');
                        if (!tbody) return;
                        if (!leads.length) {
                            tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted">No leads available. Ingest one to test.</td></tr>';
                            return;
                        }
                        tbody.innerHTML = leads.map(l => `
                            <tr>
                                <td><b>${l.fullName}</b></td>
                                <td>${l.phoneNumber}</td>
                                <td><span class="badge bg-secondary">${l.vertical || 'GENERAL'}</span></td>
                                <td><small class="text-muted">${l.source}</small></td>
                                <td><b class="text-success">${l.aiLeadScore || 0}/100</b></td>
                                <td><span class="badge badge-${(l.intentCategory || 'cold').toLowerCase()}">${l.intentCategory || 'COLD'}</span></td>
                                <td><button class="btn btn-sm btn-success" onclick="callCustomer('${l.phoneNumber}')"><i class="fa fa-phone"></i></button></td>
                            </tr>
                        `).join('');
                    }

                    function loadLeadsFiltered(type) {
                        switchView('dashboard');
                        loadLeadsTable(type ? `/api/v1/leads?type=${type}` : '/api/v1/leads');
                    }

                    function loadVertical(vertical) {
                        switchView('dashboard');
                        loadLeadsTable(`/api/v1/leads?vertical=${vertical}`);
                    }

                    async function loadCallsTable() {
                        const res = await fetch('/api/v1/telephony/calls');
                        const calls = await res.json();
                        const tbody = document.getElementById('callsTbody');
                        if (!tbody) return;
                        tbody.innerHTML = calls.map(c => `
                            <tr>
                                <td><code>${c.channelId}</code></td>
                                <td>${c.callerNumber}</td>
                                <td><i class="fa fa-file-audio text-info me-1"></i> ${c.recordingFileName || 'call.wav'}</td>
                                <td>${c.initiatedAt ? new Date(c.initiatedAt).toLocaleTimeString() : 'Active'}</td>
                            </tr>
                        `).join('');
                    }

                    async function callCustomer(num) {
                        alert('Origination signal sent to Asterisk Softphone for destination: ' + num);
                        await fetch(`/api/v1/telephony/originate?customerNumber=${encodeURIComponent(num)}&agentExt=1001`, {method: 'POST'});
                    }

                    function originateCallDirect() {
                        const val = document.getElementById('dialInput').value;
                        if (val) callCustomer(val);
                    }

                    async function runCopilot() {
                        const ctx = document.getElementById('cpContext').value;
                        const st = document.getElementById('cpStatement').value;
                        const out = document.getElementById('cpOutput');
                        out.innerHTML = '<h5 class="text-primary"><i class="fa fa-spinner fa-spin me-2"></i>Ollama AI reasoning...</h5>';
                        const res = await fetch(`/api/v1/ai/copilot?leadContext=${encodeURIComponent(ctx)}&customerStatement=${encodeURIComponent(st)}`, {method: 'POST'});
                        const d = await res.json();
                        out.innerHTML = `
                            <h5 class="text-success"><i class="fa fa-check-circle me-1"></i> Recommended Strategy</h5>
                            <p><b>Action:</b> ${d.recommendedAction}</p>
                            <p><b>Rebuttal:</b> ${d.objectionsHandling}</p>
                            <div class="p-3 bg-dark border border-success rounded mt-3">
                                <b>Say to customer:</b><br>"${d.closingScript}"
                            </div>
                        `;
                    }

                    async function dispatchWhatsApp() {
                        const recipients = document.getElementById('waRecipients').value;
                        const template = document.getElementById('waTemplate').value;
                        const lang = document.getElementById('waLang').value;
                        const res = await fetch('/api/v1/campaigns/whatsapp/bulk', {
                            method: 'POST',
                            headers: {'Content-Type': 'application/json'},
                            body: JSON.stringify({ recipients, template, lang })
                        });
                        const data = await res.json();
                        document.getElementById('waResult').innerHTML = `<div class="alert alert-success">Successfully dispatched ${data.total} WhatsApp messages!</div>`;
                    }

                    // Default View
                    switchView('dashboard');
                </script>
            </body>
            </html>
            """;
    }
}