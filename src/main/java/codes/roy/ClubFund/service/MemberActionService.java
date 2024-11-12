package codes.roy.ClubFund.service;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import codes.roy.ClubFund.model.Contribution;
import codes.roy.ClubFund.model.Investment;
import codes.roy.ClubFund.model.Member;
import codes.roy.ClubFund.model.MutualFund;
import codes.roy.ClubFund.repository.AllDataRepository;
import codes.roy.ClubFund.util.ClubFundUtil;

@Service
@Transactional
public class MemberActionService {

	@Autowired AllDataRepository repo;
	@Autowired ClubFundUtil util;

	public ObjectNode getContributions(int invId) {
		ObjectMapper objMapper = new ObjectMapper();
		ObjectNode invDetailsNode = objMapper.createObjectNode();
		Investment investment = repo.getInvestmentById(invId);
		if(investment==null) {
			return null;
		}
		// contributions
		List<Contribution> contributions = investment.getContributionList();
		ArrayNode contributionsNode = objMapper.createArrayNode();
		for(Contribution contri : contributions) {
			ObjectNode contriNode = objMapper.createObjectNode();
			contriNode.put("member_name", contri.getContributor().getName());
			contriNode.put("ammount", contri.getAmmount());
			contributionsNode.add(contriNode);
		}
		invDetailsNode.set("contributions", contributionsNode);

		return invDetailsNode;
	}

	public ObjectNode getInvestmentsByYear(int year) {
		List<Investment> investments = repo.getInvestmentsByYear(year);
		ObjectMapper objMapper = new ObjectMapper();
		ObjectNode wrapperNode = objMapper.createObjectNode();
		DecimalFormat decimalFormat = new DecimalFormat("#.##");
		ArrayNode investmentsNode = objMapper.createArrayNode();
		List<Contribution> contributions;
		int invNo = 0;
		for(Investment inv : investments) {
			if(invNo==0) invNo = repo.getInvNo(inv);
			else invNo--;
			ObjectNode invNode = objMapper.createObjectNode();
			invNode.put("investment_no", invNo);
			invNode.put("investment_id", inv.getInvestmentId());
			invNode.put("date", inv.getInvestmentDate().toString());
			invNode.put("fund_id", inv.getFund().getFundId());
			invNode.put("ammount", decimalFormat.format(inv.getAmmountInvested()));
			invNode.put("current_value", decimalFormat.format(inv.getCurrentValue()));
			invNode.put("returns", decimalFormat.format(inv.getReturns()));
			invNode.put("have_nav_values", inv.isNavValuesPresent());
			contributions = inv.getContributionList();
			ArrayNode contributionsNode = objMapper.createArrayNode();
			for(Contribution contri : contributions) {
				ObjectNode contriNode = objMapper.createObjectNode();
				contriNode.put("member_id", contri.getContributor().getMemberId());
				contriNode.put("ammount", contri.getAmmount());
				contributionsNode.add(contriNode);
			}
			invNode.set("contributions", contributionsNode);
			investmentsNode.add(invNode);
		}
		wrapperNode.set("investments", investmentsNode);
		return wrapperNode;
	}

	public ObjectNode getOverview(int memberId) {
		ObjectMapper objMapper = new ObjectMapper();
		ObjectNode overviewNode = objMapper.createObjectNode();
		HashMap<Integer, LinkedHashMap<LocalDate, Double>> investWiseNC = ClubFundUtil.getInvestmentWiseNC();
		// temporary variables
		DecimalFormat decimalFormat = new DecimalFormat("#.##");
		LocalDate date;
		Double nc;
		ObjectNode ncNode;

		// current_member
		Member currentMember = repo.getMemberById(memberId);
		if(currentMember==null) {
			return null;
		}
		ArrayNode memberInvestWiseNCNode = objMapper.createArrayNode();
		if(investWiseNC.containsKey(currentMember.getMemberId())) {
			for(Entry<LocalDate, Double> entry : investWiseNC.get(currentMember.getMemberId()).entrySet()) {
				date = entry.getKey();
				nc = entry.getValue();
				ncNode = objMapper.createObjectNode();
				ncNode.put("investment_date", date.toString());
				ncNode.put("net_contribution", decimalFormat.format(nc));
				memberInvestWiseNCNode.add(ncNode);
			}
		}
		ObjectNode currentMemberNode = objMapper.createObjectNode();
		currentMemberNode.put("member_id", currentMember.getMemberId());
		currentMemberNode.put("name", currentMember.getName());
		currentMemberNode.put("email", currentMember.getEmail());
		currentMemberNode.put("cvp", decimalFormat.format(currentMember.getCvp()));
		currentMemberNode.put("cvi", decimalFormat.format(currentMember.getCvi()));
		currentMemberNode.put("nc", decimalFormat.format(currentMember.getNc()));
		currentMemberNode.put("rpi", decimalFormat.format(currentMember.getRpi()));
		currentMemberNode.put("isAdmin", currentMember.isAdmin()? "YES" : "NO");
		currentMemberNode.set("investment_wise_nc", memberInvestWiseNCNode);
		overviewNode.set("current_member", currentMemberNode);

		// others_member_details
		Member idealMember = repo.getMemberByEmail(ClubFundUtil.idealMemberEmailId);
		ArrayNode othersContributionDetailsNode = objMapper.createArrayNode();
		List<Member> members = repo.getAllMembers();
		members.sort(Comparator.<Member, Double>comparing(Member::getNc, Comparator.reverseOrder())
				.thenComparing(Comparator.comparingDouble(mem->mem.getCvi())));
		for(Member member: members) {
			if(member.getMemberId()==memberId || member.getMemberId()==idealMember.getMemberId()) {
				continue;
			}
			ObjectNode otherMemberNode = objMapper.createObjectNode();
			otherMemberNode.put("member_id", member.getMemberId());
			otherMemberNode.put("name", member.getName());
			otherMemberNode.put("email", member.getEmail());
			otherMemberNode.put("cvp", decimalFormat.format(member.getCvp()));
			otherMemberNode.put("cvi", decimalFormat.format(member.getCvi()));
			otherMemberNode.put("nc", decimalFormat.format(member.getNc()));
			otherMemberNode.put("rpi", decimalFormat.format(member.getRpi()));
			otherMemberNode.put("isAdmin", member.isAdmin()? "YES" : "NO");
			othersContributionDetailsNode.add(otherMemberNode);
		}
		overviewNode.set("others_member_details", othersContributionDetailsNode);

		// net_fund_details
		HashMap<String, Double> clubFundDetails = ClubFundUtil.getClubFundDetails();
		ObjectNode netFundDetailsNode = objMapper.createObjectNode();
		netFundDetailsNode.put("current_value", decimalFormat.format(clubFundDetails.get("current_value")));
		netFundDetailsNode.put("invested", decimalFormat.format(clubFundDetails.get("net_investment")));
		netFundDetailsNode.put("total_returns", decimalFormat.format(clubFundDetails.get("net_returns")));
		netFundDetailsNode.put("xirr", decimalFormat.format(clubFundDetails.get("xirr")));
		overviewNode.set("net_fund_details", netFundDetailsNode);

		// mutual_fund_details
		Set<Integer> latestNavFundIds = new HashSet<>(repo.getFundIdsHavingLatestNav());
		List<MutualFund> mutualFunds = repo.getAllMutualFundsInOrder();
		ArrayNode mutualFundDetailsNode = objMapper.createArrayNode();
		for(MutualFund fund : mutualFunds) {
			ObjectNode mfNode = objMapper.createObjectNode();
			mfNode.put("fund_id", fund.getFundId());
			mfNode.put("fund_name", fund.getFundName());
			mfNode.put("current_value", decimalFormat.format(fund.getCurrentValue()));
			mfNode.put("invested", decimalFormat.format(fund.getNetInvestment()));
			mfNode.put("total_returns", decimalFormat.format(fund.getNetReturns()));
			mfNode.put("xirr", decimalFormat.format(fund.getXirr()));
			mfNode.put("api_url", fund.getApiURL());
			mfNode.put("has_latest_nav_value", latestNavFundIds.contains(fund.getFundId()));
			mutualFundDetailsNode.add(mfNode);
		}
		overviewNode.set("mutual_fund_details", mutualFundDetailsNode);

		// investments
		ArrayNode investmentsNode = objMapper.createArrayNode();
		List<Integer> invYears = repo.getInvestmentYears();
		List<Investment> investments;
		if(invYears.size()>0) {
			investments = repo.getInvestmentsByYear(invYears.get(0));
		} else {
			investments = new ArrayList<>();
		}
		List<Contribution> contributions;
		int invNo = 0;
		for(Investment inv : investments) {
			if(invNo==0) invNo = repo.getInvNo(inv);
			else invNo--;
			ObjectNode invNode = objMapper.createObjectNode();
			invNode.put("investment_no", invNo);
			invNode.put("investment_id", inv.getInvestmentId());
			invNode.put("date", inv.getInvestmentDate().toString());
			invNode.put("fund_id", inv.getFund().getFundId());
			invNode.put("ammount", decimalFormat.format(inv.getAmmountInvested()));
			invNode.put("current_value", decimalFormat.format(inv.getCurrentValue()));
			invNode.put("returns", decimalFormat.format(inv.getReturns()));
			invNode.put("has_nav_values", inv.isNavValuesPresent());
			contributions = inv.getContributionList();
			ArrayNode contributionsNode = objMapper.createArrayNode();
			for(Contribution contri : contributions) {
				ObjectNode contriNode = objMapper.createObjectNode();
				contriNode.put("member_id", contri.getContributor().getMemberId());
				contriNode.put("ammount", contri.getAmmount());
				contributionsNode.add(contriNode);
			}
			invNode.set("contributions", contributionsNode);
			investmentsNode.add(invNode);
		}
		
		overviewNode.set("investments", investmentsNode);

		//investment_years
		ArrayNode investmentYearsNode = objMapper.createArrayNode();
		for(int year : repo.getInvestmentYears()) {
			investmentYearsNode.add(year);
		}
		overviewNode.set("investment_years", investmentYearsNode);
		
		return overviewNode;
	}

}
