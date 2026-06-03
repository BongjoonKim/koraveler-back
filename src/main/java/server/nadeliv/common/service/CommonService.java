package server.nadeliv.common.service;

import com.fasterxml.jackson.databind.JsonNode;
import server.nadeliv.common.dto.OgMetadataDTO;

public interface CommonService {
    String getWeatherData() throws Exception;

    OgMetadataDTO getOgMetadata(String url) throws Exception;
}
