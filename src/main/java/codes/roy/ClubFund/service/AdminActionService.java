package codes.roy.ClubFund.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import codes.roy.ClubFund.model.Contribution;
import codes.roy.ClubFund.model.Investment;
import codes.roy.ClubFund.model.Member;
import codes.roy.ClubFund.model.MutualFund;
import codes.roy.ClubFund.model.MutualFundNav;
import codes.roy.ClubFund.repository.AllDataRepository;
import codes.roy.ClubFund.util.ClubFundUtil;
import codes.roy.ClubFund.util.MutualFundAPIUtil;

@Service
@Transactional
public class AdminActionService {

	@Autowired AllDataRepository repo;
	@Autowired ClubFundUtil util;
	@Autowired MutualFundAPIUtil mfAPI;
	@Autowired BCryptPasswordEncoder pwdEncoder;

	// Member services
	public void addMember(Member newMember) {
		newMember.setPwdHash(pwdEncoder.encode(newMember.getPwdHash()));
		repo.addMember(newMember);
		util.doCalculations();
	}
	public void updateMember(Member updatedMember) {
		Member member = repo.getMemberById(updatedMember.getMemberId());
		member.setName(updatedMember.getName());
		member.setEmail(updatedMember.getEmail());
		member.setAdmin(updatedMember.isAdmin());
		repo.updateMember(member);
	}

	// MutualFund services
	public void addMutualFund(MutualFund newMutualFund) {
		repo.addMutualFund(newMutualFund);
		LocalDate latestNavDate = repo.getLatestNavDate();
		if(latestNavDate==null) {
			mfAPI.fetchAllLatestNavs();
		} else {
			mfAPI.fetchAndSaveRequiredFundNavs(latestNavDate, false);
		}

	}
	public void updateMutualFund(MutualFund updatedMutualFund) {
		MutualFund fund = repo.getMutualFundById(updatedMutualFund.getFundId());
		fund.setFundName(updatedMutualFund.getFundName());
		fund.setApiURL(updatedMutualFund.getApiURL());
		repo.updateMutualFund(fund);
	}
	public String removeMutualFund(int fundId) {
		MutualFund fund = repo.getMutualFundById(fundId);
		if(fund==null) {
			return "Invalid Fund Id";
		}
		if(repo.getInvCount(fund)>0) {
			return "There are Investments with this fund.";
		}
		repo.deleteAllNavsOfFund(fundId);
		repo.deleteMutualFund(fund);
		return "SUCCESS";
	}

	// Investment services
	public String addInvestment(Investment newInvestment) {
		DayOfWeek day = newInvestment.getInvestmentDate().getDayOfWeek();
		if(day==DayOfWeek.SATURDAY || day==DayOfWeek.SUNDAY) {
			return newInvestment.getInvestmentDate() + " is a " + day;
		}
		if(!repo.isValidFundId(newInvestment.getFund().getFundId())) {
			return newInvestment.getFund().getFundId() + " is not a valid Fund Id";
		}
		Set<Integer> latestNavFundIds = new HashSet<>(repo.getFundIdsHavingLatestNav());
		if(!latestNavFundIds.contains(newInvestment.getFund().getFundId())) {
			return "fund does not have latest nav";
		}
		boolean hasIdealContribution = false;
		int idealMemberId = repo.getMemberByEmail(ClubFundUtil.idealMemberEmailId).getMemberId();
		int ammountInvested = 0;
		for(Contribution contribution : newInvestment.getContributionList()) {
			if(!repo.isValidMemberId(contribution.getContributor().getMemberId())) 
				return contribution.getContributor().getMemberId() + " is not a valid Member Id";
			if(contribution.getContributor().getMemberId()==idealMemberId) hasIdealContribution = true;
			else ammountInvested += contribution.getAmmount();
			contribution.setInvestment(newInvestment);
		}
		if(!hasIdealContribution) {
			return "Missing Ideal Contribution";
		}
		newInvestment.setAmmountInvested(ammountInvested);
		newInvestment.setCurrentValue(ammountInvested);
		newInvestment.setReturns(0);
		newInvestment.setNavValuesPresent(false);
		repo.addInvestment(newInvestment);

		newInvestment.setNavValuesPresent(mfAPI.fetchAndSaveRequiredFundNavs(newInvestment.getInvestmentDate(), true));

		if(newInvestment.isNavValuesPresent()) {
			mfAPI.fetchAllLatestNavs();
			MutualFund fund = repo.getMutualFundById(newInvestment.getFund().getFundId());
			fund.setNetInvestment(fund.getNetInvestment()+newInvestment.getAmmountInvested());
			repo.updateMutualFund(fund);
			util.doCalculations();
		}

		return "SUCCESS";
	}

	public boolean removeInvestment(int investmentId) {
		Investment investment = repo.getInvestmentById(investmentId);
		if(investment==null) {
			return false;
		}
		repo.deleteInvestment(investment);
		repo.detachNavsFromInvestments(investment.getInvestmentDate());
		if(investment.isNavValuesPresent()) {
			MutualFund fund = investment.getFund();
			fund.setNetInvestment(fund.getNetInvestment()-investment.getAmmountInvested());
			repo.updateMutualFund(fund);
			util.doCalculations();
		}
		return true;
	}

	// Nav Services

	public boolean fetchNavsFromAPI(LocalDate date) {
		List<Investment> invs = repo.getInvestmentsOn(date);
		boolean isInvDate = invs.size()>0;
		boolean status = mfAPI.fetchAndSaveRequiredFundNavs(date, isInvDate);
		if(status) {
			for(Investment inv : invs) {
				if(!inv.isNavValuesPresent()) {
					inv.setNavValuesPresent(true);
					MutualFund fund = repo.getMutualFundById(inv.getFund().getFundId());
					fund.setNetInvestment(fund.getNetInvestment()+inv.getAmmountInvested());
					repo.updateMutualFund(fund);
					repo.updateInvestment(inv);
				}
			}
			util.doCalculations();
		}

		return status;
	}

	public ObjectNode getNavs(LocalDate date) {
		ObjectMapper objMapper = new ObjectMapper();
		ObjectNode wrapperNode = objMapper.createObjectNode();
		ArrayNode navValuesNode = objMapper.createArrayNode();
		List<MutualFund> funds = repo.getFundsRequiringNav(date);
		for(MutualFund fund : funds) {
			MutualFundNav nav = repo.getNav(fund, date);
			ObjectNode navValueNode = objMapper.createObjectNode();
			navValueNode.put("fund_id", fund.getFundId());
			navValueNode.put("nav_value", "");
			if(nav!=null) {
				navValueNode.put("nav_value", String.valueOf(nav.getNav()));
			}
			navValuesNode.add(navValueNode);
		}
		wrapperNode.set("nav_values", navValuesNode);
		return wrapperNode;
	}

	public String setNavs(JsonNode reqData) {
		LocalDate date;
		HashMap<Integer, Double> navDetailsMap = new HashMap<>();
		Double navValue;
		int fundId;
		// Parse reqData
		try {
			date = LocalDate.parse(reqData.get("date").asText());
			DayOfWeek day = date.getDayOfWeek();
			if(day==DayOfWeek.SATURDAY || day==DayOfWeek.SUNDAY) {
				return date + " is a " + day;
			}
			for(JsonNode navDetails : reqData.get("nav_values")) {
				navValue = Double.parseDouble(navDetails.get("nav_value").asText());
				fundId = navDetails.get("fund_id").asInt();
				if(fundId==0) {
					return navDetails.get("fund_id").toString() + " is not a valid fund id";
				}
				navDetailsMap.put(fundId, navValue);
			}
		} catch(Exception ex) {
			ex.printStackTrace();
			return "Unable to parse date/nav value";
		}

		// checking if all required nav values are passed
		List<MutualFund> funds = repo.getFundsRequiringNav(date);
		for(MutualFund fund : funds) {
			if(!navDetailsMap.containsKey(fund.getFundId())) {
				return "All required nav must be set at once";
			}
		}

		// set nav values
		List<Investment> invs = repo.getInvestmentsOn(date);
		boolean isInvDate = invs.size()>0;
		for(MutualFund fund : funds) {
			fundId = fund.getFundId();
			navValue = navDetailsMap.get(fundId);
			MutualFundNav nav = repo.getNav(fund, date);
			if(nav==null) {
				nav = new MutualFundNav(fund, date, navValue, isInvDate);
			} else {
				nav.setNav(navValue);
				if(!nav.isInvestmentDate()) {
					nav.setInvestmentDate(isInvDate);
				}
			}
			repo.updateNav(nav);
		}
		for(Investment inv : invs) {
			if(!inv.isNavValuesPresent()) {
				inv.setNavValuesPresent(true);
				repo.updateInvestment(inv);
			}
		}
		util.doCalculations();

		return "SUCCESS";
	}

}
