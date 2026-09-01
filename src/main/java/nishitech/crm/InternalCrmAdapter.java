package nishitech.crm;

import nishitech.entity.CallLog;
import nishitech.entity.Lead;
import nishitech.repository.CallLogRepository;
import nishitech.repository.LeadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InternalCrmAdapter implements CrmAdapter {
    private final LeadRepository leadRepository;
    private final CallLogRepository callLogRepository;

    @Override
    public void syncLead(Lead lead) {
        leadRepository.save(lead);
    }

    @Override
    public void logCall(CallLog callLog) {
        callLogRepository.save(callLog);
    }

    @Override
    public String getProviderName() {
        return "INTERNAL";
    }
}