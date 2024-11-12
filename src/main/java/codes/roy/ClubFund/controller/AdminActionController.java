package codes.roy.ClubFund.controller;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import codes.roy.ClubFund.model.Investment;
import codes.roy.ClubFund.model.Member;
import codes.roy.ClubFund.model.MutualFund;
import codes.roy.ClubFund.service.AdminActionService;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/admin")
public class AdminActionController {

	private static final Logger LOGGER = LoggerFactory.getLogger(AdminActionController.class);

	@Autowired
	AdminActionService service;

	// Member end-points

	@PostMapping("/addmember")
	public void addMember(@RequestBody Member newMember) {
		service.addMember(newMember);
	}

	@PostMapping("/updatemember")
	public void updateMember(@RequestBody Member updatedMember) {
		service.updateMember(updatedMember);
	}

	// MutualFund end-points

	@PostMapping("/addfund")
	public void addMutualFund(@RequestBody MutualFund newFund) {
		service.addMutualFund(newFund);
	}

	@PostMapping("/updatefund")
	public void updateMutualFund(@RequestBody MutualFund updatedFund) {
		service.updateMutualFund(updatedFund);
	}

	@PostMapping("/removefund")
	public int removeMutualFund(@RequestBody JsonNode reqData, HttpServletResponse response) {
		int status = 0;
		try {
			int fundId = reqData.get("fund_id").asInt();
			String serviceResponse = service.removeMutualFund(fundId);
			if (serviceResponse.equals("SUCCESS")) {
				status = 1;
			} else {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.getWriter().write(serviceResponse);
			}
		} catch (IOException e) {
			LOGGER.error("removefund controller exception : ", e);
		}
		return status;
	}

	// Investment end-points

	@PostMapping("/addinvestment")
	public void addInvestment(@RequestBody Investment newInvestment, HttpServletResponse response) {
		String status = service.addInvestment(newInvestment);
		try {
			if (!status.equals("SUCCESS")) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.getWriter().write(status);
			}

		} catch (IOException e) {
			LOGGER.error("addinvestment controller exception : ", e);
		}
	}

	@PostMapping("/removeinvestment")
	public void removeInvestment(@RequestBody Investment investment, HttpServletResponse response) {
		boolean status = service.removeInvestment(investment.getInvestmentId());
		try {
			if (!status){
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.getWriter().write("Invalid Investment");
			}
		} catch (IOException e) {
			LOGGER.error("removeinvestment controller exception : ", e);
		}
	}

	// Nav end-points

	@PostMapping("/fetchnavs")
	public int fetchNavs(@RequestBody JsonNode reqData, HttpServletResponse response) {
		LocalDate date = null;
		try {
			date = LocalDate.parse(reqData.get("date").asText());
		} catch (DateTimeParseException dtpex) {
			dtpex.printStackTrace();
			try {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.getWriter().write("Invalid Date");
			} catch (IOException e) {
				LOGGER.error("fetchnavs controller exception : ", e);
			}
		}
		int result = 0;
		if (date != null && service.fetchNavsFromAPI(date)) {
			result = 1;
		}
		return result;
	}

	@PostMapping("/getnavs")
	public ObjectNode getNavs(@RequestBody JsonNode reqData, HttpServletResponse response) {
		LocalDate date = null;
		try {
			date = LocalDate.parse(reqData.get("date").asText());
		} catch (DateTimeParseException dtpex) {
			dtpex.printStackTrace();
			try {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.getWriter().write("Invalid Date");
			} catch (IOException e) {
				LOGGER.error("getnavs controller exception : ", e);
			}
		}
		ObjectNode result = null;
		if (date != null) {
			result = service.getNavs(date);
		}
		return result;
	}

	@PostMapping("/setnavs")
	public void setNavs(@RequestBody JsonNode navDetails, HttpServletResponse response) {
		String status = service.setNavs(navDetails);
		try {
			if (!status.equals("SUCCESS")) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.getWriter().write(status);
			}
		} catch (IOException e) {
			LOGGER.error("setnavs controller exception : ", e);
		}
	}

}
