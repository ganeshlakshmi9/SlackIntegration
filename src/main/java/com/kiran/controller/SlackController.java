package com.kiran.controller;

import com.kiran.controller.dto.RetroDTO.RetroDTO;
import com.kiran.model.entity.RetroEntity;
import com.kiran.model.response.ReadAllRetroResponse;
import com.kiran.model.response.SlackResponse;
import com.kiran.service.RetroService;
import com.kiran.service.SlackService;
import com.kiran.service.exception.InvalidMove;
import com.kiran.service.integration.DictionaryAPI;
import com.kiran.service.integration.JiraAPI;
import com.kiran.service.utilities.SlackAsyncService;
import com.kiran.service.utilities.Utilities;
import com.kiran.translator.RetroTranslator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * @author Kiran
 * @since 8/24/17
 */

@RestController
@RequestMapping("/slack")
public class SlackController {

    @Autowired
    private Utilities utilities;

    @Autowired
    private SlackService slackService;

    @Autowired
    private JiraAPI jiraAPI;

    @Autowired
    private DictionaryAPI dictionaryAPI;

    @Autowired
    private SlackAsyncService slackAsyncService;


    @Autowired
    private RetroService retroService;

    @Autowired
    private RetroTranslator retroTranslator;


    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    // Health Check
    @RequestMapping(method = RequestMethod.GET)
    public String healthCheck() {
        return "<marquee behavior=\"alternate\" scrollamount=\"5\">I...</marquee>\n" +
                "<marquee behavior=\"alternate\" scrollamount=\"6\">AM...</marquee>\n" +
                "<marquee behavior=\"alternate\" scrollamount=\"7\">WORKING...</marquee>\n" +
                "<marquee behavior=\"alternate\" scrollamount=\"8\">FINE!</marquee>\n" +
                "\n";
    }


    //Slack================================

    @RequestMapping(value = "/jira/issue", method = RequestMethod.POST, consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE})
    public HttpEntity<?> getIssueDetail(@RequestBody MultiValueMap<String, String> formVars) {
        logger.info("Inside /jira/issue controller------------------------------------");
        try {
            String userName = utilities.trimString(formVars.get("user_name").toString(), 1);
            String text = utilities.trimString(formVars.get("text").toString(), 1);
            slackAsyncService.logInDB(userName, text);
            if (!text.isEmpty()) {
                String jiraTicket = slackService.getJiraCode(text);
                if (jiraTicket == null) {
                    SlackResponse response = new SlackResponse("Please input a valid Jira Ticket");
                    return new ResponseEntity<>(response, null, HttpStatus.OK);
                }
                HashMap<String, String> jiraMap;
                jiraMap = slackService.getJiraResponse(jiraTicket);
                String responseString = "*Ticket* : " + jiraTicket + "\n*Summary* : " + jiraMap.get("summary") + "\n*Assignee* : " + jiraMap.get("assignee") + "\n*Status* : " + jiraMap.get("status");
                if (jiraMap.get("lastComment") != null) {
                    responseString += "\n*LastComment* : " + jiraMap.get("lastComment");
                }
                SlackResponse response = new SlackResponse(responseString);
                return new ResponseEntity<>(response, null, HttpStatus.OK);

            } else {
                SlackResponse response = new SlackResponse("Welcome, " + userName.substring(0, 1).toUpperCase() + userName.substring(1) + ". You can now look for Jira Ticket Info.");
                return new ResponseEntity<>(response, null, HttpStatus.OK);
            }
        } catch (InvalidMove e) {
            SlackResponse response = new SlackResponse("Something went wrong. Please try again");
            return new ResponseEntity<>(response, null, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Please contact your administrator", null, HttpStatus.INTERNAL_SERVER_ERROR);
        } finally {
            logger.info("Exiting /jira/issue controller------------------------------------");
        }
    }


    @RequestMapping(value = "/jira/assignee", method = RequestMethod.POST, consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE})
    public HttpEntity<?> assignTicket(@RequestBody MultiValueMap<String, String> formVars) {
        logger.info("Inside /jira/assignee controller------------------------------------");
        try {
            String userName = utilities.trimString(formVars.get("user_name").toString(), 1);
            String text = utilities.trimString(formVars.get("text").toString(), 1);
            slackAsyncService.logInDB(userName, text);
            if (!text.isEmpty()) {
                String jiraTicket = slackService.getJiraCode(text);
                String assigneeName = slackService.getAssigneeName(text);
                if (jiraTicket == null) {
                    SlackResponse response = new SlackResponse("Please input a valid Jira Ticket");
                    return new ResponseEntity<>(response, null, HttpStatus.OK);
                }
                if (assigneeName == null) {
                    SlackResponse response = new SlackResponse("Format not correct.\nUser \"XXXX-3333 to user:JiraUserName\"");
                    return new ResponseEntity<>(response, null, HttpStatus.OK);
                }
                String passed = jiraAPI.assignATicket(jiraTicket, assigneeName);
                if (passed.equals("passed")) {
                    SlackResponse response = new SlackResponse("Ticket: *" + jiraTicket + "* is assigned to *" + assigneeName + "*");
                    return new ResponseEntity<>(response, null, HttpStatus.OK);
                } else if (passed.equals("userIssue")) {
                    SlackResponse response = new SlackResponse("User: *" + assigneeName + "* does not exist.");
                    return new ResponseEntity<>(response, null, HttpStatus.OK);
                } else if (passed.equals("jiraTicket")) {
                    SlackResponse response = new SlackResponse("Jira Ticket is invalid");
                    return new ResponseEntity<>(response, null, HttpStatus.OK);
                } else {
                    SlackResponse response = new SlackResponse("Something went wrong. Please try again.");
                    return new ResponseEntity<>(response, null, HttpStatus.OK);
                }

            } else {
                SlackResponse response = new SlackResponse("Welcome, " + userName.substring(0, 1).toUpperCase() + userName.substring(1) + ". You can now Assign a Jira Ticket");
                return new ResponseEntity<>(response, null, HttpStatus.OK);
            }
        } catch (InvalidMove e) {
            SlackResponse response = new SlackResponse("Something went wrong. Please try again");
            return new ResponseEntity<>(response, null, HttpStatus.OK);
        } catch (Exception e) {
            SlackResponse response = new SlackResponse("Something went wrong. Please contact your administrator.");
            return new ResponseEntity<>(response, null, HttpStatus.OK);
        } finally {
            logger.info("Exiting /jira/assignee controller------------------------------------");
        }
    }

    @RequestMapping(value = "/food", method = RequestMethod.POST, consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE})
    public HttpEntity<?> getRestaurantsDetails(@RequestBody MultiValueMap<String, String> formVars) throws InterruptedException {
        logger.info("Inside /food controller------------------------------------");
        try {
            String userName = utilities.trimString(formVars.get("user_name").toString(), 1);
            String text = utilities.trimString(formVars.get("text").toString(), 1);
            String responseUrl = utilities.trimString(formVars.get("response_url").toString(), 1);
            SlackResponse responseOk = new SlackResponse("Searching...");
            slackAsyncService.logInDB(userName, text);
            slackAsyncService.postMessage(userName, text, responseUrl);
            return new ResponseEntity<>(responseOk, null, HttpStatus.OK);
        } finally {
            logger.info("Exiting /food controller------------------------------------");
        }
    }


    @RequestMapping(value = "/regression", method = RequestMethod.POST, consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE})
    public HttpEntity<?> runRegression(@RequestBody MultiValueMap<String, String> formVars) throws InterruptedException {
        logger.info("Inside /regression controller------------------------------------");
        try {
            String userName = utilities.trimString(formVars.get("user_name").toString(), 1);
            String text = utilities.trimString(formVars.get("text").toString(), 1);
            String[] splited = text.split("\\s+");
            String responseUrl = utilities.trimString(formVars.get("response_url").toString(), 1);
            SlackResponse responseOk = new SlackResponse("Testing " + splited[0] + "...... Branch: " + splited[1]);
            slackAsyncService.logInDB(userName, text);
            slackAsyncService.regressionTestResponse(splited[0], splited[1], splited[2], responseUrl);
            return new ResponseEntity<>(responseOk, null, HttpStatus.OK);
        } catch (Exception e) {
            logger.error(e.getMessage());
            SlackResponse response = new SlackResponse("Something went wrong. Please contact your administrator.");
            return new ResponseEntity<>(response, null, HttpStatus.OK);
        } finally {
            logger.info("Exiting /regression controller------------------------------------");
        }
    }


    //Read all message
    @RequestMapping(method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<?> readRetros() {
        Iterable<RetroEntity> retroEntityIterable = retroService.readAllActiveRetro();
        List<RetroDTO> retroEntityList =
                retroTranslator.entityListToDTOList(retroEntityIterable);
        ReadAllRetroResponse response = new ReadAllRetroResponse(retroEntityList);
        return new ResponseEntity<>(response, null, HttpStatus.OK);
    }


    //CreateRetro
    @RequestMapping(value = "/retros", method = RequestMethod.POST, consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<?> createRetro(@RequestBody MultiValueMap<String, String> formVars) {
        try {
            String userName = utilities.trimString(formVars.get("user_name").toString(), 1);
            String text = utilities.trimString(formVars.get("text").toString(), 1);
            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            Date date = new Date();
            String timeStamp = dateFormat.format(date);
            RetroEntity entity = new RetroEntity(userName, text, timeStamp, true);
            slackAsyncService.logInDB(userName, text);
            retroService.createRetro(entity);
            SlackResponse response = new SlackResponse("Your message has been saved anonymously", true);
            return new ResponseEntity<>(response, null, HttpStatus.OK);
        } catch (Exception e) {
            logger.error(e.getMessage());
            SlackResponse response = new SlackResponse("Something went wrong. Please contact your administrator.", true);
            return new ResponseEntity<>(response, null, HttpStatus.OK);
        }
    }

    //Clear all message
    @RequestMapping(value = "/retros/clear", method = RequestMethod.POST, consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<?> clearRetros(@RequestBody MultiValueMap<String, String> formVars) {
        try {
            String userName = utilities.trimString(formVars.get("user_name").toString(), 1);
            String text = utilities.trimString(formVars.get("text").toString(), 1);
            slackAsyncService.logInDB(userName, text);
            String message = "";
            if (!(userName.equalsIgnoreCase("kiran") || userName.equalsIgnoreCase("psagiraju"))) {
                message = "You do not have enough rights for this call.";
            } else {
                Iterable<RetroEntity> retroEntityIterable = retroService.readAllActiveRetro();
                for (RetroEntity e : retroEntityIterable) {
                    e.setActive(false);
                    retroService.createRetro(e);
                }
                message = "All Previous Retros has been cleared.";
            }
            SlackResponse response = new SlackResponse(message, true);
            return new ResponseEntity<>(response, null, HttpStatus.OK);
        } catch (Exception e) {
            logger.error(e.getMessage());
            SlackResponse response = new SlackResponse("Something went wrong. Please contact your administrator.", true);
            return new ResponseEntity<>(response, null, HttpStatus.OK);
        }

    }

    //Read all message
    @RequestMapping(value = "/retros/read", method = RequestMethod.POST, consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<?> readRetros(@RequestBody MultiValueMap<String, String> formVars) {
        try {
            List<RetroEntity> retroEntityList = retroService.readAllActiveRetro();
            String message = "";
            if (retroEntityList.size() == 0) {
                message = "\n*There is not any message at the moment.*\n";
            } else {
                message = "\n*-------All Retro Messages-------*\n";
                int i = 1;
                for (RetroEntity e : retroEntityList) {
                    message += "*" + i + ". " + e.getRetroMessage() + "*\n";
                    i++;
                }
            }
            String finalMessage = utilities.trimString(message, 1);
            SlackResponse response = new SlackResponse(finalMessage);
            return new ResponseEntity<>(response, null, HttpStatus.OK);
        } catch (Exception e) {
            logger.error(e.getMessage());
            SlackResponse response = new SlackResponse("Something went wrong. Please contact your administrator.", true);
            return new ResponseEntity<>(response, null, HttpStatus.OK);
        }

    }


    @RequestMapping(value = "/dictionary/read", method = RequestMethod.POST, consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<?> getMeaning(@RequestBody MultiValueMap<String, String> formVars) {
        try {
            String userName = utilities.trimString(formVars.get("user_name").toString(), 1);
            String text = utilities.trimString(formVars.get("text").toString(), 1);
            String sentence = dictionaryAPI.getSentence(text);
            String meaning = dictionaryAPI.getMeaning(text);
            SlackResponse response = new SlackResponse("*" + text.toUpperCase() + "*" + "\n\n*Meaning:* " + meaning + "\n*Sentence:* " + sentence+"\n:beer:");
            return new ResponseEntity<>(response, null, HttpStatus.OK);
        } catch (Exception e) {
            logger.error(e.getMessage());
            SlackResponse response = new SlackResponse("Error. Probably your word doesn't exists. Try again.", true);
            return new ResponseEntity<>(response, null, HttpStatus.OK);
        }
    }


}


