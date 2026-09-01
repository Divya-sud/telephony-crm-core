package nishitech.crm;

import nishitech.entity.CallLog;
import nishitech.entity.Lead;

public interface CrmAdapter {
    void syncLead(Lead lead);
    void logCall(CallLog callLog);
    String getProviderName();
}