package codes.roy.ClubFund.service;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import codes.roy.ClubFund.bean.MemberTokenDetails;
import codes.roy.ClubFund.model.Member;
import codes.roy.ClubFund.repository.AllDataRepository;
import codes.roy.ClubFund.util.AccessTokenUtil;
import codes.roy.ClubFund.util.ClubFundUtil;

@Service
public class PublicActionService {

	@Autowired AllDataRepository repo;
	@Autowired AccessTokenUtil tokenUtil;
	@Autowired BCryptPasswordEncoder pwdEncoder;

	public String getAuthToken(JsonNode reqData) {
		String result = "ERR:UNKNOWN";
		try {
			String email = reqData.get("email").asText();
			if(email.equals(ClubFundUtil.idealMemberEmailId) && repo.getAdminCount()>0) {
				return "ERR:Admin member present";
			}
			Member member = repo.getMemberByEmail(email);
			if(member==null) {
				return "ERR:Invalid Credentials";
			}
			String givenPwd = reqData.get("password").asText();
			if(email.equals(ClubFundUtil.idealMemberEmailId)){
				if(!ClubFundUtil.validateIdealMemberPwd(givenPwd)) {
					return "ERR:Invalid Credentials";
				}
			} else if(!pwdEncoder.matches(givenPwd, member.getPwdHash())) {
				return "ERR:Invalid Credentials";
			}

			result = tokenUtil.createToken(new MemberTokenDetails(member.getMemberId(), email, member.isAdmin()));

		} catch(NullPointerException ex) {
			ex.printStackTrace();
			return "ERR:email & password are mandatory parameters";
		}
		return result;
	}
	
	public ObjectNode getPublicData() {
		ObjectMapper objMapper = new ObjectMapper();
		ObjectNode publicDataNode = objMapper.createObjectNode();
		HashMap<Integer, LinkedHashMap<LocalDate, Double>> investWiseNC = ClubFundUtil.getInvestmentWiseNC();
		// temporary variables
		DecimalFormat decimalFormat = new DecimalFormat("#.##");
		LocalDate date;
		Double nc;
		ObjectNode ncNode;

		// ideal_member
		Member idealMember = repo.getMemberByEmail(ClubFundUtil.idealMemberEmailId);
		ArrayNode idealInvestWiseNCNode = objMapper.createArrayNode();
		if(investWiseNC.containsKey(idealMember.getMemberId())) {
			for(Entry<LocalDate, Double> entry : investWiseNC.get(idealMember.getMemberId()).entrySet()) {
				date = entry.getKey();
				nc = entry.getValue();
				ncNode = objMapper.createObjectNode();
				ncNode.put("investment_date", date.toString());
				ncNode.put("net_contribution", decimalFormat.format(nc));
				idealInvestWiseNCNode.add(ncNode);
			}
		}
		ObjectNode idealMemberNode = objMapper.createObjectNode();
		idealMemberNode.put("id", decimalFormat.format(idealMember.getMemberId()));
		idealMemberNode.put("cvp", decimalFormat.format(idealMember.getCvp()));
		idealMemberNode.put("cvi", decimalFormat.format(idealMember.getCvi()));
		idealMemberNode.put("nc", decimalFormat.format(idealMember.getNc()));
		idealMemberNode.put("rpi", repo.getCurrentIdealContributionAmmount());
		idealMemberNode.set("investment_wise_nc", idealInvestWiseNCNode);
		publicDataNode.set("ideal_member", idealMemberNode);
		
		// max_nc
		publicDataNode.put("max_nc", decimalFormat.format(ClubFundUtil.getMaxNC()));
		
		// latest_nav_date
		LocalDate latestNavDate = repo.getLatestNavDate();
		publicDataNode.put("latest_nav_date",(latestNavDate==null)? "" : latestNavDate.toString());
		
		
		return publicDataNode;
	}
}
