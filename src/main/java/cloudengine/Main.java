package cloudengine;

import com.github.wnameless.json.flattener.JsonFlattener;
import org.drools.core.base.RuleNameStartsWithAgendaFilter;
import org.json.simple.JSONObject;
import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

import java.util.HashMap;
import java.util.Map;

public class Main {

    public static void main(String[] args) {
        Map<String, Object> subData = new HashMap<String, Object>();
        subData.put("arn", "test@m11.zhang");


        Map<String, Object> data = new HashMap<String, Object>();
        data.put("name", "Test");
        data.put("eventVersion", "1.05");
        data.put("userIdentity", subData);
        data.put("eventSource", "ec2.amazonaws.com");
        data.put("eventName", "AuthorizeSecurityGroupIngress");
        data.put("userAgent", "console.awsamazon.com");

        JSONObject j = new JSONObject(data);
        j.putAll(j);
        System.out.printf("JSON: %s", j.toJSONString());

        KieServices kieServices = KieServices.Factory.get();
        KieContainer kieContainer = kieServices.getKieClasspathContainer();
        KieSession kSession = kieContainer.newKieSession("ksession-rules");
        kSession.insert(JsonFlattener.flattenAsMap(j.toJSONString()));
        kSession.fireAllRules(new RuleNameStartsWithAgendaFilter((String) j.get("eventSource")));
    }
}
