/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.portfolio.loanproduct.calc;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.apache.fineract.organisation.monetary.domain.Money;
import org.apache.fineract.portfolio.loanaccount.data.LoanTermVariationsData;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepaymentScheduleInstallment;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanScheduleModelRepaymentPeriod;
import org.apache.fineract.portfolio.loanproduct.calc.data.OutstandingDetails;
import org.apache.fineract.portfolio.loanproduct.calc.data.PeriodDueDetails;
import org.apache.fineract.portfolio.loanproduct.calc.data.ProgressiveLoanInterestScheduleModel;
import org.apache.fineract.portfolio.loanproduct.calc.data.RepaymentPeriod;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProductMinimumRepaymentScheduleRelatedDetail;

public interface EMICalculator {

    /**
     * This method creates an Interest model with repayment periods from the schedule periods which generated by
     * schedule generator.
     */
    @NotNull
    ProgressiveLoanInterestScheduleModel generatePeriodInterestScheduleModel(@NotNull List<LoanScheduleModelRepaymentPeriod> periods,
            @NotNull LoanProductMinimumRepaymentScheduleRelatedDetail loanProductRelatedDetail,
            List<LoanTermVariationsData> loanTermVariations, Integer installmentAmountInMultiplesOf, MathContext mc);

    /**
     * This method creates an Interest model with repayment periods from the installments which retrieved from the
     * database.
     */
    @NotNull
    ProgressiveLoanInterestScheduleModel generateInstallmentInterestScheduleModel(
            @NotNull List<LoanRepaymentScheduleInstallment> installments,
            @NotNull LoanProductMinimumRepaymentScheduleRelatedDetail loanProductRelatedDetail,
            List<LoanTermVariationsData> loanTermVariations, Integer installmentAmountInMultiplesOf, MathContext mc);

    /**
     * Find repayment period based on Due Date.
     */
    Optional<RepaymentPeriod> findRepaymentPeriod(ProgressiveLoanInterestScheduleModel scheduleModel, LocalDate dueDate);

    /**
     * Applies the disbursement on the interest model. This method recalculates the EMI amounts from the action date.
     */
    void addDisbursement(ProgressiveLoanInterestScheduleModel scheduleModel, LocalDate disbursementDueDate, Money disbursedAmount);

    /**
     * Applies the interest rate change on the interest model. This method recalculates the EMI amounts from the action
     * date.
     */
    void changeInterestRate(ProgressiveLoanInterestScheduleModel scheduleModel, LocalDate newInterestSubmittedOnDate,
            BigDecimal newInterestRate);

    /**
     * This method applies outstanding balance correction on the interest model. Negative amount decreases the
     * outstanding balance while positive amounts are increasing that. Typically used for late repayment or to count
     * repayments.
     */
    void addBalanceCorrection(ProgressiveLoanInterestScheduleModel scheduleModel, LocalDate balanceCorrectionDate,
            Money balanceCorrectionAmount);

    /**
     * This method used for pay interest portion during the repayment transaction.
     */
    void payInterest(ProgressiveLoanInterestScheduleModel scheduleModel, LocalDate repaymentPeriodDueDate, LocalDate transactionDate,
            Money interestAmount);

    /**
     * This method used for pay principal portion during the repayment transaction.
     */
    void payPrincipal(ProgressiveLoanInterestScheduleModel scheduleModel, LocalDate repaymentPeriodDueDate, LocalDate transactionDate,
            Money principalAmount);

    /**
     * This method used for charge back principal portion. This method increases the outstanding balance. This method
     * creates a calculated "virtual" EMI for the applied period.
     */
    void chargebackPrincipal(ProgressiveLoanInterestScheduleModel scheduleModel, LocalDate transactionDate,
            Money chargebackPrincipalAmount);

    /**
     * This method used for charge back interest portion. This method adds extra interest due. This method creates a
     * calculated "virtual" EMI for the applied period.
     */
    void chargebackInterest(ProgressiveLoanInterestScheduleModel scheduleModel, LocalDate transactionDate, Money chargebackInterestAmount);

    /**
     * This method gives back the maximum of the due principal and maximum of the due interest for a requested day.
     */
    @NotNull
    PeriodDueDetails getDueAmounts(@NotNull ProgressiveLoanInterestScheduleModel scheduleModel, @NotNull LocalDate periodDueDate,
            @NotNull LocalDate targetDate);

    /**
     * Gives back the sum of the interest from the whole model on the given date.
     */
    @NotNull
    Money getPeriodInterestTillDate(@NotNull ProgressiveLoanInterestScheduleModel scheduleModel, @NotNull LocalDate periodDueDate,
            @NotNull LocalDate targetDate, boolean includeChargebackInterest);

    Money getOutstandingLoanBalanceOfPeriod(ProgressiveLoanInterestScheduleModel interestScheduleModel, LocalDate repaymentPeriodDueDate,
            LocalDate targetDate);

    OutstandingDetails getOutstandingAmountsTillDate(ProgressiveLoanInterestScheduleModel model, LocalDate targetDate);

    void calculateRateFactorForRepaymentPeriod(RepaymentPeriod repaymentPeriod, ProgressiveLoanInterestScheduleModel scheduleModel);

    Money getSumOfDueInterestsOnDate(ProgressiveLoanInterestScheduleModel scheduleModel, LocalDate subjectDate);

    /**
     * This method stops the interest counting for the given range. Chargeback interest counts even if the normal
     * interest paused.
     */
    void applyInterestPause(ProgressiveLoanInterestScheduleModel scheduleModel, LocalDate fromDate, LocalDate endDate);
}
