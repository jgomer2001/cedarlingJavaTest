package co;

import io.jans.cedarling.binding.wrapper.CedarlingAdapter;
import io.jans.util.Pair;

import java.nio.file.*;
import java.util.*;
import java.security.*;

import org.json.*;
import uniffi.cedarling_uniffi.*;

//mvn -o exec:java -Dexec.mainClass=co.MyCedarling -Dexec.args="1000 config.json ey....blah..."
//
public class MyCedarling {
    
    private CedarlingAdapter cedarlingAdapter = new CedarlingAdapter();   
    private Random ranma = new SecureRandom();

    public static void main(String ...args) throws Exception {
        new MyCedarling().run(Integer.parseInt(args[0]), args[1], args[2]);        
    }
    
    private void run(int n, String propertiesFile, String userInfoToken) throws Exception {
        
        String config = Files.readString(Paths.get(propertiesFile));
        cedarlingAdapter.loadFromJson(config);
        log("Cedarling initialized with\n" + config);
        
        Map<String, String> tokens = Map.of("Jans::Userinfo_token", userInfoToken);
        int ignored = 0;
        
        long decisionJava = 0, decisionNative = 0;
        for (int i = 0; i < n; i++) {
            //Generate random student with grad_year in [2024, 2027]
            Map<String, Object> student = rndStudent("" + i, 2024, 2028);
            
            Pair<Long, Long> times = authorize(tokens, "Jans::Action::\"Search\"", student, new JSONObject());
            Long tNative = times.getSecond();
            
            if (tNative != null) {
                decisionNative += tNative;
                decisionJava += times.getFirst();
            } else {
                //ignore this authorization if native time was missing in cedarling log
                ignored++;
            }            
        }
        int total = n - ignored;
        log("Native average decision time (μs): " + String.format("%.3f", 0.001f * decisionNative / total));
        log("Java average decision time (μs): " + String.format("%.3f", 0.001f * decisionJava / total));
        log("Total evaluations: " + total);
        
    }
    
    private Pair<Long, Long> authorize(Map<String, String> tokens, String action, Map<String, Object> resource,
            JSONObject context) throws Exception {

        List<TokenInput> tokenInputs = new ArrayList<>();
        tokens.entrySet().forEach(e -> tokenInputs.add(new TokenInput(e.getKey(), e.getValue())));        
            
        long elapsed = System.nanoTime();
        MultiIssuerAuthorizeResult res = cedarlingAdapter.authorizeMultiIssuer(tokenInputs, action,
                new JSONObject(resource), context);
        elapsed = System.nanoTime() - elapsed;     

        List<String> decisionLogs = cedarlingAdapter.getLogsByRequestId(res.getRequestId());
        Long nativeTime = null;
        
        if (decisionLogs.isEmpty()) return new Pair<>(elapsed, null);
                
        JSONObject job = new JSONObject(decisionLogs.get(0));
        nativeTime = job.optLongObject("decision_time_micro_sec", null);
    
        return new Pair<>(elapsed, 1000*nativeTime);    //nano seconds

    }
    
    private Map<String, Object> rndStudent(String id, int min, int max) {
        
        Map<String, Object> student = new HashMap<>(
                Map.of("grad_year", getADecimal(min, max))
        );
        student.putAll(
            Map.of("cedar_entity_mapping",
                Map.of(
                    "entity_type", "Jans::student",
                    "id", id
                )
            )
        );
        student.put("name", "Joe " + id);
        return student;
        
    }
    
    private int getADecimal(int min, int max) {
        //Pick a uniformly distributed random number from the range [min, max)
        return ranma.nextInt(max - min) + min;
    }
    
    private static void log(String msg) {
        System.out.println(msg);
    }

    
}
