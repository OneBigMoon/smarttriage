/**
  * Copyright 2021 bejson.com 
  */
package com.wcjk.triage.bean.ltrasonic;
import java.util.List;

/**
 * Auto-generated: 2021-05-10 16:20:17
 *
 * @author bejson.com (i@bejson.com)
 * @website http://www.bejson.com/java2pojo/
 */
public class QueuesBean {

    private List<Patients> patients;
    private String queuename;
    public void setPatients(List<Patients> patients) {
         this.patients = patients;
     }
     public List<Patients> getPatients() {
         return patients;
     }

    public void setQueuename(String queuename) {
         this.queuename = queuename;
     }
     public String getQueuename() {
         return queuename;
     }

}