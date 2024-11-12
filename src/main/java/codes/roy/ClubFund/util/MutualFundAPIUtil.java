package codes.roy.ClubFund.util;

import java.time.LocalDate;

public interface MutualFundAPIUtil {

	// method to fetch latest nav for all the funds in db
	public boolean fetchAllLatestNavs();

	// method to fetch nav for given date for all funds in db. Note - isInvestmentDate should be if there is a investment on this date
	public boolean fetchAndSaveRequiredFundNavs(LocalDate date, boolean isInvestmentDate);

}
