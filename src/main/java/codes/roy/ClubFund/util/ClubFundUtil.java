package codes.roy.ClubFund.util;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.decampo.xirr.Transaction;
import org.decampo.xirr.Xirr;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.nimbusds.jose.util.Base64;

import codes.roy.ClubFund.model.Contribution;
import codes.roy.ClubFund.model.Investment;
import codes.roy.ClubFund.model.Member;
import codes.roy.ClubFund.model.MutualFund;
import codes.roy.ClubFund.model.MutualFundNav;
import codes.roy.ClubFund.repository.AllDataRepository;


@Component
@Transactional
public class ClubFundUtil implements ApplicationRunner {

	@Autowired AllDataRepository repo;
	@Autowired MutualFundAPIUtil mfAPI;
	@Autowired BCryptPasswordEncoder pwdEncoder;

	public static final String idealMemberEmailId = "ideal@club.fund";
	private static final String idealMemberDefaultPwd = "Pass@8910";
	private static final String secretStr = "rVqnzFaQrkiOqRMiO4+jbQ==";
	private final static LocalDate maturityDate = LocalDate.parse("2034-12-25");
	private final static String issuer = "clubfund.rivals.com";

	private static byte secretBytes[];
	private static int investmentPattern = 0; // 0 : Monthly/ 1 : Yearly
	private static Double maxNC;
	private static HashMap<String, Double> clubFundDetails = new HashMap<>();
	private static HashMap<Integer, LinkedHashMap<LocalDate, Double>> investmentWiseNC = new HashMap<>();

	@Override
	public void run(ApplicationArguments args) throws Exception {
		mfAPI.fetchAllLatestNavs();
		doCalculations();
		createIdealMemberIfMissing();
		System.out.println(clubFundDetails);
	}

	@Scheduled(fixedRate = 1000*3600*1, initialDelay = 1000*3600*1)
	public void fetchAllLatestNavs(){
		LocalDate currDate = LocalDate.now();
		DayOfWeek day = currDate.getDayOfWeek();
		LocalDate latestNavDate = repo.getLatestNavDate();
		if((latestNavDate==null) || currDate.minusDays(1).isEqual(latestNavDate) || (day==DayOfWeek.SUNDAY && currDate.minusDays(2).isEqual(latestNavDate))) {
			return;
		}
		if(day==DayOfWeek.MONDAY && currDate.minusDays(3).isEqual(latestNavDate)) {
			return;
		}
		if(mfAPI.fetchAllLatestNavs()) {
			doCalculations();
		}
	}

	@Scheduled(fixedRate = 1000*3600*24*10)
	public void deleteUnnecessaryNavs() {
		repo.deleteUnnecessaryNavs();
	}

	public void doCalculations() {
		calculateInvAndMFDetails(); // Investment and MutualFund related calculations
		calculateClubFundDetails(); // Club Fund calculations
		calculateMemberDetails(); // calculate investmentWiseNC & cvp, cvi & nc values for all members
		calculateRPIForAllMembers(); // calculate rpi for all members
	}

	public void calculateRPIForAllMembers() {
		List<Member> allMembers = repo.getAllMembers();
		for(Member mem: allMembers) {
			if(mem.getEmail().equals(idealMemberEmailId)) {
				continue;
			}
			mem.setRpi(calculateRPI(mem));
			repo.updateMember(mem);
		}
	}

	public void calculateMemberDetails() {

		HashMap<Integer, LinkedHashMap<LocalDate, Double>> tmpInvestmentWiseNC = new HashMap<>();
		// creating navMap (Map of all navs of investment dates)
		HashMap<Integer, HashMap<LocalDate, Double>> navMap = new HashMap<>();
		List<MutualFundNav> invNavs = repo.getAllInvNav();
		for(MutualFundNav nav : invNavs) {
			if(!navMap.containsKey(nav.getFund().getFundId())) {
				navMap.put(nav.getFund().getFundId(), new HashMap<>());
			}
			navMap.get(nav.getFund().getFundId()).put(nav.getDate(), nav.getNav());
		}

		// creating memContriMap. Using memContriMap and navMap to fill tmpInvestmentWiseNC
		HashMap<Integer, List<HashMap<String, Object>>> memContriMap = new HashMap<>();
		List<Investment> invs = repo.getAllInvestmentsInASC();
		List<Member> members = repo.getAllMembers();

		for(Member mem : members) {
			if(!memContriMap.containsKey(mem.getMemberId())) {
				memContriMap.put(mem.getMemberId(), new ArrayList<>());
			}
		}

		// below are tmp variables only
		int fundId;
		double nc, currMaxNC=0,ammount,contriNav,currentNav;
		List<Contribution> contributions;
		HashMap<String, Object> tmpMemberContribution;
		LocalDate invDate;
		for(Investment inv : invs) {
			if(!inv.isNavValuesPresent()) {
				continue;
			}
			contributions = inv.getContributionList();
			for(Contribution contribution : contributions) {
				tmpMemberContribution = new HashMap<>();
				tmpMemberContribution.put("fund_id", inv.getFund().getFundId());
				tmpMemberContribution.put("inv_date", inv.getInvestmentDate());
				tmpMemberContribution.put("ammount", contribution.getAmmount());
				memContriMap.get(contribution.getContributor().getMemberId()).add(tmpMemberContribution);
			}

			// memContriMap is having all member contributions including current investment contributions
			for(int memId : memContriMap.keySet()) {
				nc = 0;
				// calculating net contribution till current investment for each member
				for(HashMap<String, Object> memberContribution : memContriMap.get(memId)) {
					fundId = (int)memberContribution.get("fund_id");
					invDate = (LocalDate)memberContribution.get("inv_date");
					ammount = (int)memberContribution.get("ammount");
					if(invDate.isEqual(inv.getInvestmentDate())) {
						nc+=ammount;
					} else {
						contriNav = navMap.get(fundId).get(invDate); // nav value at the time of contribution
						currentNav = navMap.get(fundId).get(inv.getInvestmentDate()); // nav value at the time of current investment
						nc+=ammount*currentNav/contriNav;
					}
				}
				if(!tmpInvestmentWiseNC.containsKey(memId)) {
					tmpInvestmentWiseNC.put(memId,new LinkedHashMap<>());
				}
				tmpInvestmentWiseNC.get(memId).put(inv.getInvestmentDate(), nc);
				if(nc>currMaxNC) currMaxNC = nc;
			}
		}
		
		maxNC = currMaxNC;
		investmentWiseNC = tmpInvestmentWiseNC;

		// Calculating cvp,cvi & nc for all members. Note - memContriMap is having all member contributions
		HashMap<Integer, Double> latestNavMap = new HashMap<>();
		for(MutualFundNav nav : repo.getAllLatestNav()) {
			latestNavMap.put(nav.getFund().getFundId(), nav.getNav());
		}

		double cvp, cvi;
		Member currentMember;
		for(int memId : memContriMap.keySet()) {
			cvp = cvi = nc = 0;
			for(HashMap<String, Object> memberContribution : memContriMap.get(memId)) {
				fundId = (int)memberContribution.get("fund_id");
				invDate = (LocalDate)memberContribution.get("inv_date");
				ammount = (int)memberContribution.get("ammount");
				currentNav = latestNavMap.get(fundId);
				contriNav = navMap.get(fundId).get(invDate);

				cvp += ammount;
				cvi += ammount*currentNav/contriNav-ammount;
				nc += ammount*currentNav/contriNav;
			}
			currentMember = repo.getMemberById(memId);
			currentMember.setCvp(cvp);
			currentMember.setCvi(cvi);
			currentMember.setNc(nc);
			repo.updateMember(currentMember);
			if(nc>maxNC) maxNC = nc;
		}

	}

	// investment related calculations must be performed before doing Club Fund related calculations
	public void calculateClubFundDetails() {
		List<Investment> invs = repo.getAllInvestments();
		List<Transaction> transactions = new ArrayList<>();
		double netInvestment = 0, currentVal = 0, netReturns = 0, xirr = 0;
		for(Investment inv : invs) {
			if(!inv.isNavValuesPresent()) {
				continue;
			}
			netInvestment+=inv.getAmmountInvested();
			currentVal+=inv.getCurrentValue();
			transactions.add(new Transaction(inv.getAmmountInvested()*-1, inv.getInvestmentDate()));
		}
		if(netInvestment!=0) {
			transactions.add(new Transaction(currentVal, repo.getAllLatestNav().get(0).getDate()));
			xirr = new Xirr(transactions).xirr()*100;
			netReturns = (currentVal - netInvestment)/netInvestment*100;
		}

		clubFundDetails.put("net_investment", netInvestment);
		clubFundDetails.put("current_value", currentVal);
		clubFundDetails.put("net_returns", netReturns);
		clubFundDetails.put("xirr", xirr);
	}


	public void calculateInvAndMFDetails(){

		HashMap<Integer, HashMap<String, Double>> fundDetailsMap = new HashMap<>();
		HashMap<Integer, LocalDate> latestNavDateMap = new HashMap<>();
		HashMap<Integer, List<Transaction>> transactionMap = new HashMap<>();
		List<Investment> invs = repo.getAllInvestments();
		List<MutualFundNav> lastestNavs = repo.getAllLatestNav();
		int fundId;
		HashMap<String, Double> fundDetails;
		for(MutualFundNav latestNav : lastestNavs) {
			fundId = latestNav.getFund().getFundId();
			fundDetailsMap.put(fundId, new HashMap<>());
			transactionMap.put(fundId, new ArrayList<>());
			fundDetails = fundDetailsMap.get(fundId);
			fundDetails.put("latestNav", latestNav.getNav());
			latestNavDateMap.put(fundId, latestNav.getDate());
			fundDetails.put("currentValue", 0D);
		}

		double navOnInvDate;
		double currentValueOfInvestment;
		for(Investment inv : invs) {
			if(!inv.isNavValuesPresent()) continue;
			fundId = inv.getFund().getFundId();
			fundDetails = fundDetailsMap.get(fundId);
			transactionMap.get(fundId).add(new Transaction(inv.getAmmountInvested()*-1, inv.getInvestmentDate()));
			navOnInvDate = repo.getNav(inv.getFund(), inv.getInvestmentDate()).getNav();
			currentValueOfInvestment = inv.getAmmountInvested()*(fundDetails.get("latestNav")/navOnInvDate);
			inv.setCurrentValue(currentValueOfInvestment);
			inv.setReturns((currentValueOfInvestment-inv.getAmmountInvested())/inv.getAmmountInvested()*100);
			repo.updateInvestment(inv);
			fundDetails.put("currentValue", fundDetails.get("currentValue") + currentValueOfInvestment);
		}

		List<Transaction> transactionList;
		List<MutualFund> funds = repo.getAllMutualFunds();
		double currFundValue, xirr;
		for(MutualFund fund: funds) {
			transactionList = transactionMap.get(fund.getFundId());
			if(transactionList!=null && transactionList.size()>0) {
				currFundValue = fundDetailsMap.get(fund.getFundId()).get("currentValue");
				transactionList.add(new Transaction(currFundValue, latestNavDateMap.get(fund.getFundId())));
				xirr = new Xirr(transactionList).xirr()*100;
				fundDetailsMap.get(fund.getFundId()).put("xirr", xirr);
				fund.setCurrentValue(currFundValue);
				if(fund.getNetInvestment()==0) fund.setNetReturns(0);
				else fund.setNetReturns((fund.getCurrentValue() - fund.getNetInvestment())/fund.getNetInvestment()*100);
				fund.setXirr(xirr);
			} else {
				fund.setCurrentValue(0);
				fund.setNetReturns(0);
				fund.setXirr(0);
			}

			repo.updateMutualFund(fund);
		}

	}

	private double calculateRPI(Member member) {

		if(clubFundDetails.get("xirr")==null || clubFundDetails.get("xirr")==0) {
			return 0;
		}
		double avgXIRR = clubFundDetails.get("xirr")/100;
		double r = Math.pow((avgXIRR+1), 1/12D)-1; // Monthly compounding rate equivalent to avg xirr
		double memberCurrentNC = 0;
		if(member!=null && member.getNc()!=0) {
			memberCurrentNC = member.getNc();
		}
		int months = getMonthsToMaturity();
		double projectedValueOfNC = memberCurrentNC * Math.pow(1+r, months);
		double targetFutureAmmount = getProjectedIdealMaturityContribution() - projectedValueOfNC;
		double rpi = 0;
		if(investmentPattern==0) {
			rpi = targetFutureAmmount/((Math.pow(1+r, months)-1)*(1+r)/r);
		} else if(investmentPattern==1) {
			rpi = targetFutureAmmount/(Math.pow(1+avgXIRR, months/12D)-1)*(1+avgXIRR)/avgXIRR;
		}

		return rpi<0? 0 : rpi;
	}

	private double getProjectedIdealMaturityContribution() {

		if(clubFundDetails.get("xirr")==null || clubFundDetails.get("xirr")==0) {
			return 0;
		}
		double avgXIRR = clubFundDetails.get("xirr")/100;
		double r = Math.pow((avgXIRR+1), 1/12D)-1; // Monthly compounding rate equivalent to avg xirr
		Member member = repo.getMemberByEmail(idealMemberEmailId);
		if(member==null || member.getNc()==0) {
			return 0;
		}
		int months = getMonthsToMaturity();
		double projectedValueOfNC = member.getNc() * Math.pow(1+r, months);
		double projectedFutureContribution = 0;
		int perodicContributionAmmount = repo.getCurrentIdealContributionAmmount();
		if(investmentPattern==0) {
			projectedFutureContribution = perodicContributionAmmount*(Math.pow(1+r, months)-1)*(1+r)/r;
		} else if(investmentPattern==1) {
			projectedFutureContribution = perodicContributionAmmount*(Math.pow(1+avgXIRR, months/12D)-1)*(1+avgXIRR)/avgXIRR;
		}
		return projectedValueOfNC + projectedFutureContribution;
	}

	private int getMonthsToMaturity() {
		LocalDate currentDate = LocalDate.now();
		Period period = Period.between(currentDate, maturityDate);
		int months = period.getYears() * 12 + period.getMonths();
		return months;
	}

	private void createIdealMemberIfMissing() {
		SecureRandom random = new SecureRandom();
		byte[] sharedSecret = new byte[16];
		random.nextBytes(sharedSecret);
		String encodedSecret = pwdEncoder.encode(Base64.encode(sharedSecret).toString());
		byte[] bytes = Base64.from(secretStr).decode();
		Member idealMem = repo.getMemberByEmail(idealMemberEmailId);
		if(idealMem==null) {
			idealMem = new Member("Ideal Member", idealMemberEmailId, encodedSecret);
			idealMem.setAdmin(true);
			repo.addMember(idealMem);
			setSecretBytes(bytes, encodedSecret.substring(44).getBytes(StandardCharsets.US_ASCII));
		} else {
			setSecretBytes(bytes, idealMem.getPwdHash().substring(44).getBytes(StandardCharsets.US_ASCII));
		}
	}

	private void setSecretBytes(byte[] bytes, byte[] moreBytes) {
		byte[] secret = new byte[32];
		for(int i=0;i<16;i++) {
			secret[i] = bytes[i];
			secret[i+16] = moreBytes[i];
		}
		secretBytes = secret;
	}
	
	public static Double getMaxNC() {
		return maxNC;
	}
	
	public static HashMap<String, Double> getClubFundDetails() {
		return clubFundDetails;
	}

	public static HashMap<Integer, LinkedHashMap<LocalDate, Double>> getInvestmentWiseNC() {
		return investmentWiseNC;
	}

	public static byte[] getSecretBytes() {
		return secretBytes;
	}

	public static String getIssuer() {
		return issuer;
	}

	public static boolean validateIdealMemberPwd(String givenPwd) {
		return idealMemberDefaultPwd.equals(givenPwd);
	}
}
