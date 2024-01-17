/*
 * Copyright 2012-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.crunchydata.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.crunchydata.controller.RepoController;
import com.crunchydata.util.*;
import static com.crunchydata.util.SecurityUtility.getMd5;
import static com.crunchydata.util.Settings.Props;

public class dbReconcile extends Thread {
    String modColumn;
    Integer parallelDegree;
    String schemaName;
    String tableName;
    String sql;
    String tableFilter;
    String targetType;
    Integer threadNumber;
    String threadName;

    Integer nbrColumns;
    Integer nbrPKColumns;
    Integer cid;
    Integer batchNbr;

    ThreadSync ts;

    String pkList;

    Integer tid;
    String stagingTable;
    Boolean useDatabaseHash;

    public dbReconcile(Integer threadNumber, String targetType, String sql, String tableFilter, String modColumn, Integer parallelDegree, String schemaName, String tableName, Integer nbrColumns, Integer nbrPKColumns, Integer cid, ThreadSync ts, String pkList, Boolean useDatabaseHash, Integer batchNbr, Integer tid, String stagingTable) {
        this.modColumn = modColumn;
        this.parallelDegree = parallelDegree;
        this.schemaName = schemaName;
        this.tableName = tableName;
        this.sql = sql;
        this.tableFilter = tableFilter;
        this.targetType = targetType;
        this.threadNumber = threadNumber;
        this.nbrColumns = nbrColumns;
        this.nbrPKColumns = nbrPKColumns;
        this.cid = cid;
        this.ts = ts;
        this.pkList = pkList;
        this.useDatabaseHash = useDatabaseHash;
        this.batchNbr = batchNbr;
        this.tid = tid;
        this.stagingTable = stagingTable;
    }

    public void run() {

        threadName = "reconcile-"+targetType+"-"+threadNumber;
        Logging.write("info", threadName, "Start database reconcile thread");

        /////////////////////////////////////////////////
        // Connect to Repository
        /////////////////////////////////////////////////
        Connection repoConn;
        Logging.write("info", threadName, "Connecting to repository database");
        repoConn = dbPostgres.getConnection(Props,"repo", "reconcile");
        if ( repoConn == null) {
            Logging.write("severe", threadName, "Cannot connect to repository database");
            System.exit(1);
        }
        try { repoConn.setAutoCommit(false); } catch (Exception e) {
            // do nothing
        }

        /////////////////////////////////////////////////
        // Connect to Source/Target
        /////////////////////////////////////////////////
        Connection conn;

        Logging.write("info", threadName, "Connecting to " + targetType + " database");
        if (Props.getProperty(targetType + "-type").equals("oracle")) {
            conn = dbOracle.getConnection(Props,targetType);
        } else {
            conn = dbPostgres.getConnection(Props,targetType, "reconcile");
            try { conn.setAutoCommit(false); } catch (Exception e) {
                // do nothing
            }
        }
        if ( conn == null) {
            Logging.write("severe", threadName, "Cannot connect to " + targetType + " database");
            System.exit(1);
        }

        /////////////////////////////////////////////////
        // Load Reconcile Data
        /////////////////////////////////////////////////
        ResultSet rs;
        PreparedStatement stmt;
        int cntRecord = 0;
        int totalRows = 0;

        if ( parallelDegree > 1 && !modColumn.isEmpty()) {
            sql += " AND mod(" + modColumn + "," + parallelDegree +")="+threadNumber;
        }

        if (!pkList.isEmpty() && Props.getProperty("database-sort").equals("true")) {
            sql += " ORDER BY " + pkList;
        }

        try {
            RepoController rpc = new RepoController();

            conn.setAutoCommit(false);
            stmt = conn.prepareStatement(sql);
            stmt.setFetchSize(Integer.parseInt(Props.getProperty("batch-fetch-size")));
            rs = stmt.executeQuery();
            int loadRowCount = 10000;
            int observerRowCount = 10000;
            boolean firstPass = true;

            //rs.setFetchSize(Integer.parseInt(Props.getProperty("batch-fetch-size")));

            StringBuilder columnValue = new StringBuilder();

            String sqlLoad = "INSERT INTO " + stagingTable + " (table_name, pk_hash, column_hash, pk, thread_nbr, batch_nbr) VALUES (?,?,?,(?)::jsonb,?,?)";
            repoConn.setAutoCommit(false);
            PreparedStatement stmtLoad = repoConn.prepareStatement(sqlLoad);

            while (rs.next()) {
                columnValue.setLength(0);

                if (! useDatabaseHash) {
                    for (int i = 3; i < nbrColumns + 3 - nbrPKColumns; i++) {
                        columnValue.append(rs.getString(i));
                    }
                } else {
                    columnValue.append(rs.getString(3));
                }

                stmtLoad.setString(1, tableName);
                stmtLoad.setString(2, (useDatabaseHash) ? rs.getString("PK_HASH") : getMd5(rs.getString("PK_HASH")));
                stmtLoad.setString(3, (useDatabaseHash) ? columnValue.toString() : getMd5(columnValue.toString()));
                stmtLoad.setString(4, rs.getString("PK").replace(",}","}"));
                stmtLoad.setInt(5, threadNumber);
                stmtLoad.setInt(6, batchNbr);
                stmtLoad.addBatch();
                stmtLoad.clearParameters();

                cntRecord++;
                totalRows++;

                if (totalRows % Integer.parseInt(Props.getProperty("batch-commit-size")) == 0 ) {
                    stmtLoad.executeBatch();
                    stmtLoad.clearBatch();
                    repoConn.commit();
                }

                if (totalRows % loadRowCount == 0) {
                    Logging.write("info", threadName, "Loaded " + totalRows + " rows");
                }

                if (totalRows % observerRowCount == 0) {
                    if (firstPass || Boolean.parseBoolean(Props.getProperty("observer-throttle"))) {
                        firstPass = false;

                        Logging.write("info", threadName, "Wait for Observer");

                        rpc.dcrUpdateRowCount(repoConn, targetType, cid, cntRecord);

                        repoConn.commit();

                        cntRecord=0;

                        ts.ObserverWait();

                        Logging.write("info", threadName, "Cleared by Observer");
                    } else {
                        Logging.write("info", threadName, "Pause for Observer");
                        Thread.sleep(1000);
                    }
                }

                if (!firstPass) {
                    loadRowCount = Integer.parseInt(Props.getProperty("batch-load-size"));
                    observerRowCount = Integer.parseInt(Props.getProperty("observer-throttle-size"));
                }

            }

            // Loading Remaining
            if (cntRecord > 0) {
                stmtLoad.executeBatch();
                rpc.dcrUpdateRowCount(repoConn, targetType, cid, cntRecord);
            }

            rs.close();
            stmt.close();
            stmtLoad.close();

            Logging.write("info", threadName, "Complete. Total rows loaded: " + totalRows);

        } catch (Exception e) {
            Logging.write("severe", threadName, "Error loading hash rows: " + e.getMessage());
        }

        if ( targetType.equals("source")) {
            ts.sourceComplete = true;
        } else {
            ts.targetComplete = true;
        }


        /////////////////////////////////////////////////
        // Close Connections
        /////////////////////////////////////////////////
        try { repoConn.close(); } catch (Exception e) {
            // do nothing
        }
        try { conn.close(); } catch (Exception e) {
            // do nothing
        }

    }
}
