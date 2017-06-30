// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package com.microsoft.azure.sdk.iot.service.jobs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.microsoft.azure.sdk.iot.deps.serializer.JobsResponseParser;
import com.microsoft.azure.sdk.iot.deps.serializer.JobsStatisticsParser;
import com.microsoft.azure.sdk.iot.deps.serializer.TwinParser;
import com.microsoft.azure.sdk.iot.service.devicetwin.DeviceTwinDevice;
import com.microsoft.azure.sdk.iot.service.devicetwin.Pair;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Collection with the result of a job operation.
 */
public class JobResult
{
    private static final Charset DEFAULT_IOTHUB_MESSAGE_CHARSET = StandardCharsets.UTF_8;

    // List of possible jobs
    public enum JobType
    {
        scheduleDeviceMethod,
        scheduleUpdateTwin
    }

    // List of possible IoTHub response status for a job
    public enum JobStatus
    {
        unknown,
        enqueued,
        running,
        completed,
        failed,
        cancelled,
        scheduled,
        queued
    }


    // Job identifier
    private String jobId;

    // Required if jobType is updateTwin or cloudToDeviceMethod.
    // Condition for device query to get devices to execute the job on
    private String queryCondition;

    // Scheduled job start time in UTC.
    private Date createdTime;

    // System generated start time in UTC.
    private Date startTime;

    // System generated end time in UTC.
    // Represents the time the job stopped processing.
    private Date endTime;

    // Max execution time in seconds (ttl duration)
    private Long maxExecutionTimeInSeconds;

    // Required.
    // The type of job to execute.
    private JobType jobType;

    // Required.
    // The status of job to execute.
    private JobStatus jobStatus;

    // Required if jobType is cloudToDeviceMethod.
    // The method type and parameters.
    // Ignored by the json serializer if null.
    private String cloudToDeviceMethod = null;

    // Required if jobType is updateTwin.
    // The Update Twin tags and desired properties.
    // Ignored by the json serializer if null.
    private DeviceTwinDevice updateTwin = null;

    // System generated failure reason.
    // If status == failure, this represents a string containing the reason.
    private String failureReason = null;

    // System generated status message.
    // Represents a string containing a message with status about the job execution.
    private String statusMessage = null;

    // System generated statistics.
    // Different number of devices in the job.
    private JobStatistics jobStatistics = null;

    // The deviceId related to this response.
    // It can be null (e.g. in case of a parent orchestration).
    private String deviceId = null;

    // The jobId of the parent orchestration, if any.
    private String parentJobId = null;


    /**
     * CONSTRUCTOR
     *
     * @param body is a array of bytes that contains the response message for jobs
     * @throws JsonParseException if the content of body is a invalid json
     * @throws IllegalArgumentException if the provided body is null
     */
    JobResult(byte[] body) throws JsonParseException, IllegalArgumentException
    {
        /* Codes_SRS_JOBRESULT_21_001: [The constructor shall throw IllegalArgumentException if the input body is null.] */
        if(body == null)
        {
            throw new IllegalArgumentException("null body");
        }

        /* Codes_SRS_JOBRESULT_21_002: [The constructor shall parse the body using the JobsResponseParser.] */
        /* Codes_SRS_JOBRESULT_21_003: [The constructor shall throw JsonParseException if the input body contains a invalid json.] */
        String json = new String(body, DEFAULT_IOTHUB_MESSAGE_CHARSET);
        JobsResponseParser jobsResponseParser = JobsResponseParser.createFromJson(json);

        /* Codes_SRS_JOBRESULT_21_004: [The constructor shall locally store all results information in the provided body.] */
        this.jobId = jobsResponseParser.getJobId();
        this.queryCondition = jobsResponseParser.getQueryCondition();
        this.createdTime = jobsResponseParser.getCreatedTime();
        this.startTime = jobsResponseParser.getStartTime();
        this.endTime = jobsResponseParser.getEndTime();
        this.maxExecutionTimeInSeconds = jobsResponseParser.getMaxExecutionTimeInSeconds();
        this.jobType = JobType.valueOf(jobsResponseParser.getJobType().toString());
        this.jobStatus = JobStatus.valueOf(jobsResponseParser.getJobsStatus().toString());
        if(jobsResponseParser.getCloudToDeviceMethod() != null)
        {
            this.cloudToDeviceMethod = jobsResponseParser.getCloudToDeviceMethod().toJson();
        }
        TwinParser twinParser = jobsResponseParser.getUpdateTwin();
        if(twinParser != null)
        {
            this.updateTwin = new DeviceTwinDevice(twinParser.getDeviceId());
            this.updateTwin.setETag(twinParser.getETag());
            try
            {
                this.updateTwin.setTags(mapToSet(twinParser.getTagsMap()));
            }
            catch (IOException e)
            {
                /* It can happen that the response do not have tags information, just ignore it. */
            }
            this.updateTwin.setDesiredProperties(mapToSet(twinParser.getDesiredPropertyMap()));
        }
        this.failureReason = jobsResponseParser.getFailureReason();
        this.statusMessage = jobsResponseParser.getStatusMessage();
        JobsStatisticsParser jobsStatisticsParser = jobsResponseParser.getJobStatistics();
        if(jobsStatisticsParser != null)
        {
            this.jobStatistics = new JobStatistics(jobsStatisticsParser);
        }
        this.deviceId = jobsResponseParser.getDeviceId();
        this.parentJobId = jobsResponseParser.getParentJobId();
    }

    /**
     * Getter for the Job identifier
     *
     * @return Job identifier
     */
    public String getJobId()
    {
        /* Codes_SRS_JOBRESULT_21_005: [The getJobId shall return the stored jobId.] */
        return this.jobId;
    }

    /**
     * Getter for query condition
     *
     * @return the condition for device query to get devices to execute the job on
     */
    public String getQueryCondition()
    {
        /* Codes_SRS_JOBRESULT_21_006: [The getQueryCondition shall return the stored queryCondition.] */
        return this.queryCondition;
    }

    /**
     * Getter for create time
     *
     * @return the scheduled job start time in UTC
     */
    public Date getCreatedTime()
    {
        /* Codes_SRS_JOBRESULT_21_007: [The getCreatedTime shall return the stored createdTime.] */
        return this.createdTime;
    }

    /**
     * Getter for start time UTC
     *
     * @return the system generated start time in UTC
     */
    public Date getStartTime()
    {
        /* Codes_SRS_JOBRESULT_21_008: [The getStartTime shall return the stored startTime.] */
        return this.startTime;
    }

    /**
     * Getter for the end time UTC
     * Represents the time the job stopped processing
     *
     * @return the system generated end time in UTC
     */
    public Date getEndTime()
    {
        /* Codes_SRS_JOBRESULT_21_009: [The getEndTime shall return the stored endTime.] */
        return this.endTime;
    }

    /**
     * Getter for max execution time in seconds
     *
     * @return the max execution time in seconds (ttl duration)
     */
    public Long getMaxExecutionTimeInSeconds()
    {
        /* Codes_SRS_JOBRESULT_21_010: [The getMaxExecutionTimeInSeconds shall return the stored maxExecutionTimeInSeconds.] */
        return this.maxExecutionTimeInSeconds;
    }

    /**
     * Getter for the job type
     *
     * @return the type of job to execute
     */
    public JobType getJobType()
    {
        /* Codes_SRS_JOBRESULT_21_011: [The getJobType shall return the stored jobType.] */
        return this.jobType;
    }

    /**
     * Getter for the jobs status
     *
     * @return the status of this job
     */
    public JobStatus getJobStatus()
    {
        /* Codes_SRS_JOBRESULT_21_012: [The getJobStatus shall return the stored jobStatus.] */
        return this.jobStatus;
    }

    /**
     * Getter for cloud to device method json
     *
     * @return the json of cloud to device method. It is {@code null} if jobType
     * is not scheduleDeviceMethod
     */
    public String getCloudToDeviceMethod()
    {
        /* Codes_SRS_JOBRESULT_21_013: [The getCloudToDeviceMethod shall return the stored cloudToDeviceMethod.] */
        return this.cloudToDeviceMethod;
    }

    /**
     * Getter for update twin json
     *
     * @return the json of update twin. It is {@code null} if jobType
     * is not scheduleUpdateTwin
     */
    public DeviceTwinDevice getUpdateTwin()
    {
        /* Codes_SRS_JOBRESULT_21_014: [The getUpdateTwin shall return the stored updateTwin.] */
        return this.updateTwin;
    }

    /**
     * Getter for failure reason
     *
     * @return If status == failure, this represents a string containing the
     * reason. It can be {@code null}
     */
    public String getFailureReason()
    {
        /* Codes_SRS_JOBRESULT_21_015: [The getFailureReason shall return the stored failureReason.] */
        return this.failureReason;
    }

    /**
     * Getter for the status message
     *
     * @return a string containing a message with status about the job
     * execution. It can be {@code null}
     */
    public String getStatusMessage()
    {
        /* Codes_SRS_JOBRESULT_21_016: [The getStatusMessage shall return the stored statusMessage.] */
        return this.statusMessage;
    }

    /**
     * Getter for jobs statistics
     *
     * @return a set of counters with the jobs statistics
     */
    public JobStatistics getJobStatistics()
    {
        /* Codes_SRS_JOBRESULT_21_017: [The getJobStatistics shall return the stored jobStatistics.] */
        return this.jobStatistics;
    }

    /**
     * Getter for the device Id
     *
     * @return the deviceId related to this response. It can be {@code null}
     * (e.g. in case of a parent orchestration)
     */
    public String getDeviceId()
    {
        /* Codes_SRS_JOBRESULT_21_018: [The getDeviceId shall return the stored deviceId.] */
        return this.deviceId;
    }

    /**
     * Getter for the parent jobId
     *
     * @return the jobId of the parent orchestration, if any. It can be {@code null}
     */
    public String getParentJobId()
    {
        /* Codes_SRS_JOBRESULT_21_019: [The getParentJobId shall return the stored parentJobId.] */
        return this.parentJobId;
    }

    /**
     * Return a string with a pretty print json with the content of this class
     *
     * @return a String with a json that represents the content of this class
     */
    @Override
    public String toString()
    {
        /* Codes_SRS_JOBRESULT_21_020: [The toString shall return a String with a pretty print json that represents this class.] */
        Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
        return gson.toJson(this);
    }

    private Set<Pair> mapToSet(Map<String, Object> map)
    {
        Set<Pair> setPair = new HashSet<>();

        if (map != null)
        {
            for (Map.Entry<String, Object> setEntry : map.entrySet())
            {
                setPair.add(new Pair(setEntry.getKey(), setEntry.getValue()));
            }
        }

        return setPair;
    }

}