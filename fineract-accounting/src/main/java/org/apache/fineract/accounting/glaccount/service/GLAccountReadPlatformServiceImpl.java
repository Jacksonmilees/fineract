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
package org.apache.fineract.accounting.glaccount.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.accounting.common.AccountingEnumerations;
import org.apache.fineract.accounting.glaccount.data.GLAccountData;
import org.apache.fineract.accounting.glaccount.data.GLAccountDataForLookup;
import org.apache.fineract.accounting.glaccount.domain.GLAccountType;
import org.apache.fineract.accounting.glaccount.domain.GLAccountUsage;
import org.apache.fineract.accounting.glaccount.exception.GLAccountInvalidClassificationException;
import org.apache.fineract.accounting.glaccount.exception.GLAccountNotFoundException;
import org.apache.fineract.accounting.journalentry.data.JournalEntryAssociationParametersData;
import org.apache.fineract.infrastructure.codes.data.CodeValueData;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GLAccountReadPlatformServiceImpl implements GLAccountReadPlatformService {

    private static final String NAME_DECORATED_BASE_ON_HIERARCHY = "concat(substring('........................................', 1, ((LENGTH(hierarchy) - LENGTH(REPLACE(hierarchy, '.', '')) - 1) * 4)), name)";

    private final JdbcTemplate jdbcTemplate;

    private static final String SQL_AND = " and ";

    private static final class GLAccountMapper implements RowMapper<GLAccountData> {

        private final JournalEntryAssociationParametersData associationParametersData;

        GLAccountMapper(final JournalEntryAssociationParametersData associationParametersData) {
            if (associationParametersData == null) {
                this.associationParametersData = new JournalEntryAssociationParametersData();
            } else {
                this.associationParametersData = associationParametersData;
            }
        }

        public String schema() {
            StringBuilder sb = new StringBuilder();
            sb.append(
                    " gl.id as id, name as name, parent_id as parentId, gl_code as glCode, disabled as disabled, manual_journal_entries_allowed as manualEntriesAllowed, ")
                    .append("classification_enum as classification, account_usage as accountUsage, gl.description as description, ")
                    .append(NAME_DECORATED_BASE_ON_HIERARCHY).append(" as nameDecorated, ")
                    .append("cv.id as codeId, cv.code_value as codeValue ");
            if (this.associationParametersData.isRunningBalanceRequired()) {
                sb.append(",gl_j.organization_running_balance as organizationRunningBalance ");
            }
            sb.append("from acc_gl_account gl left join m_code_value cv on tag_id=cv.id ");
            if (this.associationParametersData.isRunningBalanceRequired()) {
                sb.append("left outer Join acc_gl_journal_entry gl_j on gl_j.account_id = gl.id");
            }
            return sb.toString();
        }

        @Override
        public GLAccountData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {

            final Long id = rs.getLong("id");
            final String name = rs.getString("name");
            final Long parentId = JdbcSupport.getLong(rs, "parentId");
            final String glCode = rs.getString("glCode");
            final boolean disabled = rs.getBoolean("disabled");
            final boolean manualEntriesAllowed = rs.getBoolean("manualEntriesAllowed");
            final int accountTypeId = JdbcSupport.getInteger(rs, "classification");
            final EnumOptionData accountType = AccountingEnumerations.gLAccountType(accountTypeId);
            final int usageId = JdbcSupport.getInteger(rs, "accountUsage");
            final EnumOptionData usage = AccountingEnumerations.gLAccountUsage(usageId);
            final String description = rs.getString("description");
            final String nameDecorated = rs.getString("nameDecorated");
            final Long codeId = rs.wasNull() ? null : rs.getLong("codeId");
            final String codeValue = rs.getString("codeValue");
            final CodeValueData tagId = CodeValueData.instance(codeId, codeValue);
            Long organizationRunningBalance = null;
            if (associationParametersData.isRunningBalanceRequired()) {
                organizationRunningBalance = rs.getLong("organizationRunningBalance");
            }
            return new GLAccountData().setId(id).setName(name).setParentId(parentId).setGlCode(glCode).setDisabled(disabled)
                    .setManualEntriesAllowed(manualEntriesAllowed).setType(accountType).setUsage(usage).setDescription(description)
                    .setNameDecorated(nameDecorated).setTagId(tagId).setOrganizationRunningBalance(organizationRunningBalance);
        }
    }

    @Override
    public List<GLAccountData> retrieveAllGLAccounts(final Integer accountClassification, final String searchParam, final Integer usage,
            final Boolean manualTransactionsAllowed, final Boolean disabled,
            JournalEntryAssociationParametersData associationParametersData) {
        if (accountClassification != null && !checkValidGLAccountType(accountClassification)) {
            throw new GLAccountInvalidClassificationException(accountClassification);
        }

        if (usage != null && !checkValidGLAccountUsage(usage)) {
            throw new GLAccountInvalidClassificationException(accountClassification);
        }

        final GLAccountMapper rm = new GLAccountMapper(associationParametersData);
        String sql = "select " + rm.schema();
        // append SQL statement for fetching account totals
        if (associationParametersData != null && associationParametersData.isRunningBalanceRequired()) {
            sql = sql + " and gl_j.id in (select t1.id from (select t2.account_id, max(t2.id) as id from "
                    + "(select id, max(entry_date) as entry_date, account_id from acc_gl_journal_entry where is_running_balance_calculated = true "
                    + "group by account_id desc, id) t3 inner join acc_gl_journal_entry t2 on t2.account_id = t3.account_id and t2.entry_date = t3.entry_date "
                    + "group by t2.account_id desc) t1)";
        }
        final Object[] parameterArray = new Object[3];
        int arrayPos = 0;
        boolean filtersPresent = false;
        if ((accountClassification != null) || StringUtils.isNotBlank(searchParam) || (usage != null) || (manualTransactionsAllowed != null)
                || (disabled != null)) {
            filtersPresent = true;
            sql += " where";
        }

        if (filtersPresent) {
            boolean firstWhereConditionAdded = false;
            if (accountClassification != null) {
                sql += " classification_enum = ?";
                parameterArray[arrayPos] = accountClassification.shortValue();
                arrayPos = arrayPos + 1;
                firstWhereConditionAdded = true;
            }
            if (StringUtils.isNotBlank(searchParam)) {
                if (firstWhereConditionAdded) {
                    sql += SQL_AND;
                }
                sql += " ( name like %?% or gl_code like %?% )";
                parameterArray[arrayPos] = searchParam;
                arrayPos = arrayPos + 1;
                parameterArray[arrayPos] = searchParam;
                arrayPos = arrayPos + 1;
                firstWhereConditionAdded = true;
            }
            if (usage != null) {
                if (firstWhereConditionAdded) {
                    sql += SQL_AND;
                }
                if (GLAccountUsage.HEADER.getValue().equals(usage)) {
                    sql += " account_usage = 2 ";
                } else if (GLAccountUsage.DETAIL.getValue().equals(usage)) {
                    sql += " account_usage = 1 ";
                }
                firstWhereConditionAdded = true;
            }
            if (manualTransactionsAllowed != null) {
                if (firstWhereConditionAdded) {
                    sql += SQL_AND;
                }

                sql += " manual_journal_entries_allowed = " + manualTransactionsAllowed;
                firstWhereConditionAdded = true;
            }
            if (disabled != null) {
                if (firstWhereConditionAdded) {
                    sql += SQL_AND;
                }

                sql += " disabled = " + disabled;
            }
        }

        sql += " ORDER BY gl_code ASC";

        final Object[] finalObjectArray = Arrays.copyOf(parameterArray, arrayPos);
        return this.jdbcTemplate.query(sql, rm, (Object[]) finalObjectArray);// NOSONAR
    }

    @Override
    public GLAccountData retrieveGLAccountById(final long glAccountId, JournalEntryAssociationParametersData associationParametersData) {
        try {
            final GLAccountMapper rm = new GLAccountMapper(associationParametersData);
            final StringBuilder sql = new StringBuilder();
            sql.append("select ").append(rm.schema());
            sql.append(" where gl.id = ?");
            if (associationParametersData.isRunningBalanceRequired()) {
                sql.append(" and gl_j.is_running_balance_calculated = true ")
                        .append("  ORDER BY gl_j.entry_date DESC,gl_j.id DESC LIMIT 1");
            }

            return this.jdbcTemplate.queryForObject(sql.toString(), rm, new Object[] { glAccountId });
        } catch (final EmptyResultDataAccessException e) {
            throw new GLAccountNotFoundException(glAccountId, e);
        }
    }

    @Override
    public List<GLAccountData> retrieveAllEnabledDetailGLAccounts(final GLAccountType accountType) {
        return retrieveAllGLAccounts(accountType.getValue(), null, GLAccountUsage.DETAIL.getValue(), null, false,
                new JournalEntryAssociationParametersData());
    }

    @Override
    public List<GLAccountData> retrieveAllEnabledDetailGLAccounts() {
        return retrieveAllGLAccounts(null, null, GLAccountUsage.DETAIL.getValue(), null, false,
                new JournalEntryAssociationParametersData());
    }

    private static boolean checkValidGLAccountType(final int type) {
        for (final GLAccountType accountType : GLAccountType.values()) {
            if (accountType.getValue().equals(type)) {
                return true;
            }
        }
        return false;
    }

    private static boolean checkValidGLAccountUsage(final int type) {
        for (final GLAccountUsage accountUsage : GLAccountUsage.values()) {
            if (accountUsage.getValue().equals(type)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public GLAccountData retrieveNewGLAccountDetails(final Integer type) {
        return GLAccountData.sensibleDefaultsForNewGLAccountCreation(type);
    }

    @Override
    public List<GLAccountData> retrieveAllEnabledHeaderGLAccounts(final GLAccountType accountType) {
        return retrieveAllGLAccounts(accountType.getValue(), null, GLAccountUsage.HEADER.getValue(), null, false,
                new JournalEntryAssociationParametersData());
    }

    @Override
    public List<GLAccountDataForLookup> retrieveAccountsByTagId(final Long ruleId, final Integer transactionType) {
        final GLAccountDataLookUpMapper mapper = new GLAccountDataLookUpMapper();
        final String sql = "Select " + GLAccountDataLookUpMapper.LOOKUP_SCHEMA + " where rule.id=? and tags.acc_type_enum=?";
        return this.jdbcTemplate.query(sql, mapper, (Object[]) new Object[] { ruleId, transactionType });// NOSONAR
    }

    private static final class GLAccountDataLookUpMapper implements RowMapper<GLAccountDataForLookup> {

        private static final String LOOKUP_SCHEMA = " gl.id as id, gl.name as name, gl.gl_code as glCode from acc_accounting_rule rule join acc_rule_tags tags on tags.acc_rule_id = rule.id join acc_gl_account gl on gl.tag_id=tags.tag_id";

        @Override
        public GLAccountDataForLookup mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {
            final Long id = JdbcSupport.getLong(rs, "id");
            final String name = rs.getString("name");
            final String glCode = rs.getString("glCode");

            return new GLAccountDataForLookup().setId(id).setName(name).setGlCode(glCode);
        }
    }
}
