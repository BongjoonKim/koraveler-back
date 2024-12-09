package server.koraveler.common.service.CommonServiceImpl;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import server.koraveler.common.service.CommonService;

@Service
public class CommonServiceImpl implements CommonService {
    @Value("${spring.weather.url}")
    private String weatherUrl;

    @Value("${spring.weather.key}")
    private String weatherKey;

    @Override
    public String getWeatherData() throws Exception {
        RestTemplate restTemplate = new RestTemplate();

        String url = weatherUrl + "/current?access_key=" + weatherKey + "&query=Seoul";
        try {
            ResponseEntity<String> resData = restTemplate.getForEntity(url, String.class);
            return resData.getBody();
        } catch (Exception e) {
            throw e;
        }
    }
}
