package codes.roy.ClubFund.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.node.ObjectNode;

import codes.roy.ClubFund.service.MemberActionService;
import jakarta.servlet.http.HttpServletResponse;

@RestController()
@RequestMapping("/member")
public class MemberActionController {

	@Autowired
	MemberActionService service;

	@RequestMapping("/getcontributions")
	public ObjectNode getContributions(@RequestParam int investmentId, HttpServletResponse res) {
		ObjectNode invDetails = service.getContributions(investmentId);
		if(invDetails==null) {
			try {
				res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid Investment Id");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return invDetails;
	}

	@RequestMapping("/getinvestmentsbyyear")
	public ObjectNode getInvestmentsByYear(@RequestParam int year) {
		return service.getInvestmentsByYear(year);
	}

	@RequestMapping("/getoverview")
	public ObjectNode getOverview(@RequestParam int memberId, HttpServletResponse res) {
		ObjectNode result = service.getOverview(memberId);
		if(result==null) {
			try { res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid Member Id"); }
			catch(Exception ex) { ex.printStackTrace(); }
		}
		return result;
	}
}
