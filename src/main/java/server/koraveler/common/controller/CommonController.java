package server.koraveler.common.controller;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import server.koraveler.common.service.CommonService;

@RestController
@RequestMapping("ps/commons")
@Slf4j
public class CommonController {
    @Autowired
    private CommonService commonService;

    @GetMapping("/weather")
    public ResponseEntity<String> getWeather() {
        try {
            return ResponseEntity.ok(commonService.getWeatherData());
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.valueOf("error"), e.getMessage());
        }
    }
}
