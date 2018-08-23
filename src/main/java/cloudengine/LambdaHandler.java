package cloudengine;

import com.amazonaws.services.lambda.runtime.Context;
import com.github.wnameless.json.flattener.JsonFlattener;
import org.drools.core.base.RuleNameStartsWithAgendaFilter;
import org.json.simple.JSONObject;
import org.kie.api.KieServices;
import org.kie.api.runtime.KieSession;

import java.util.Map;

public class LambdaHandler {
    public String handleRequest(Map<String, Object> input, Context context) {
        KieSession kieSession = KieServices.Factory.get().getKieClasspathContainer().newKieSession("ksession-rules");
        kieSession.insert(JsonFlattener.flattenAsMap(new JSONObject((Map) input.get("detail")).toJSONString()));
        kieSession.fireAllRules(new RuleNameStartsWithAgendaFilter((String) ((Map) input.get("detail")).get("eventSource")));
        return null;
    }
}