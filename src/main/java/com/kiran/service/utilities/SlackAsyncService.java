package com.kiran.service.utilities;

import com.kiran.controller.dto.SlackDTO.SlackResponseAttachment;
import com.kiran.controller.dto.UserLogDTO.UserLogDTO;
import com.kiran.model.response.SlackResponse;
import com.kiran.service.SlackService;
import com.kiran.service.exception.InvalidMove;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

/**
 * @author Kiran
 * @since 9/5/17
 */

@Service
public class SlackAsyncService {

    @Autowired
    private SlackService slackService;

    @Async
    public void postMessage(String userName, String text, String responseUrl) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (!text.isEmpty()) {
            try {
                HashMap<Integer, HashMap<String, String>> restaurants = slackService.get_restaurant_list(text);
                SlackResponseAttachment slackResponseAttachment = slackService.createSlackResponse(restaurants);
                HttpEntity<SlackResponseAttachment> entity = new HttpEntity<SlackResponseAttachment>(slackResponseAttachment, headers);
                restTemplate.exchange(responseUrl, HttpMethod.POST, entity, String.class);
            } catch (InvalidMove e) {
                String errorMessage = e.getError_message();
                SlackResponse responseSlack = new SlackResponse(errorMessage);
                HttpEntity<SlackResponse> entity = new HttpEntity<SlackResponse>(responseSlack, headers);
                restTemplate.exchange(responseUrl, HttpMethod.POST, entity, String.class);
            } catch (Exception e) {
                SlackResponse responseSlack = new SlackResponse("Please contact your administrator");
                HttpEntity<SlackResponse> entity = new HttpEntity<SlackResponse>(responseSlack, headers);
                restTemplate.exchange(responseUrl, HttpMethod.POST, entity, String.class);
            }
        } else {
            SlackResponse responseSlack = new SlackResponse("Welcome, " + userName.substring(0, 1).toUpperCase() + userName.substring(1) + ". You can now search restaurants.");
            HttpEntity<SlackResponse> entity = new HttpEntity<SlackResponse>(responseSlack, headers);
            restTemplate.exchange(responseUrl, HttpMethod.POST, entity, String.class);
        }
    }

    @Async
    public void logInDB(String userName, String text) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        String timeStamp = dateFormat.format(date);
        UserLogDTO userLogDTO = new UserLogDTO(userName,timeStamp,text);
    }
}
