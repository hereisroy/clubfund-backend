package codes.roy.ClubFund.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import codes.roy.ClubFund.service.PublicActionService;
import jakarta.servlet.http.HttpServletResponse;

@RestController
public class PublicActionController {

	@Autowired PublicActionService service;

	@PostMapping("/gettoken")
	public String getAuthToken(@RequestBody JsonNode reqData, HttpServletResponse res) {
		String jws = service.getAuthToken(reqData);
		try {
			if(jws.startsWith("ERR:")) {
				res.sendError(HttpServletResponse.SC_FORBIDDEN, jws.substring(4));
			}
		} catch(IOException ex) {}
		return jws;
	}
	
	@RequestMapping("/getpublicdata")
	public ObjectNode getIdealContribution() {
		ObjectNode result = service.getPublicData();
		return result;
	}

}
