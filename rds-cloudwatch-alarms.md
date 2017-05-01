Setting up Cloudwatch alarms for RDS instances
===========================================
This document describes how to setup up [Cloudwatch](http://aws.amazon.com/cloudwatch/) alarms for RDS instances.

To view a list of available cloudwatch metrics for RDS, run the following command
```bash
$ aws cloudwatch list-metrics --namespace "AWS/RDS"
```
Design
------
RDS instances post CPU utilization, binary log usage, number of database connections, free storage, sway usage and read/write latency. Alarms are setup to send notifications when the threshold for metrics are breached. When state of an alarm changes from `OK` to `ALARM`, a [SNS](http://aws.amazon.com/sns/) notification is sent to subscribers of the SNS topic.
Setup
-----
The following steps need to be completed to setup up alarms for every set of servers - for example, demo servers for PHIX.

AWS command line tools should be installed on system used to setup these alarms.

Create an IAM group with permissions for cloudwatch and sns actions.
```bash
$ aws iam create-group --group-name aws-monitoring
```
This should return an output like this
```json
{
  "Group": {
      "Path": "/",
      "CreateDate": "2014-05-04T20:27:27.972Z",
      "GroupId": "AIDAJS2O6N2YPAR7IBIWO",
      "Arn": "arn:aws:iam::504357334963:group/aws-monitoring",
      "GroupName": "aws-monitoring"
  }
}
```
Create a group policy
```bash
$ aws iam put-group-policy --group-name aws-monitoring --policy-name aws-monitoring --policy-document file://aws-monitoring.json
```
`policy-document` defines the group policy. It's contents look like this
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "cloudwatch:*",
        "sns:*"
      ],
      "Resource": [
        "*"
      ]
    }
  ]
}
```
Create an IAM user and assign it to this group. Create access key for the user.
```bash
$ aws iam create-user --user-name aws-monitor
$ aws iam add-user-group --group-name aws-monitoring --user-name aws-monitor
$ aws iam create-access-key --user-name aws-monitor
```
The last command will produce an output similar to this
```json
{
    "AccessKey": {
        "UserName": "aws-monitor",
        "Status": "Active",
        "CreateDate": "2014-05-04T20:42:26.421Z",
        "SecretAccessKey": "wJalrXUtnFEMI/K7MDENG/bPxRfiCYzEXAMPLEKEY",
        "AccessKeyId": "AKIDPMS9RO4H3FEXAMPLE"
    }
}
```
The access and secret keys will be required to push metrics from servers to cloudwatch.

`NOTE:` New instances should be launched with an IAM role with same permissions as the above group.


#### Create SNS topic
```bash
$ aws sns create-topic --name phix-demo-alert-topic
```
This will return the topic ARN (Amazon Resource Name).
```json
{
    "TopicArn": "arn:aws:sns:us-east-1:504357334963:phix-demo-alert-topic"
}
```
#### Subscribe to topic
Subscribe email address to the topic, using the ARN above, to receive notifications.
```bash
$ aws sns subscribe --topic-arn arn:aws:sns:us-east-1:504357334963:phix-demo-alert-topic --protocol email --notification-endpoint satyendra.sharma@hcentive.com
```
SNS will return the following -
```json
{
    "SubscriptionArn": "pending confirmation"
}
```
Click on the confirmation link in the email sent by SNS.

Do this for all email addresses that neet to be subscribed to the topic.

#### Check subscription
Run the following command to check subscriptions for the topic -
```bash
$ aws sns list-subscriptions-by-topic --topic-arn arn:aws:sns:us-east-1:504357334963:phix-demo-alert-topic
```
SNS will return a JSON response with subscriptions listed in the `Subscriptions` array.
```json
{
    "Subscriptions": [
        {
            "Owner": "504357334963",
            "Endpoint": "satyendra.sharma@hcentive.com",
            "Protocol": "email",
            "TopicArn": "arn:aws:sns:us-east-1:504357334963:phix-demo-alert-topic",
            "SubscriptionArn": "arn:aws:sns:us-east-1:504357334963:phix-demo-alert-topic:b5554b20-8531-4b2f-b69f-7a41df1caac5"
        }
    ]
}
```
#### Test notifications
Publish a test message to the topic to verify everything is working as expected.
```bash
$ aws sns publish --message "Verification" --topic arn:aws:sns:us-east-1:504357334963:phix-demo-alert-topic
```
SNS will return a response like this -
```json
{
    "MessageId": "42f189a0-3094-5cf6-8fd7-c2dde61a4d7d"
}
```
Check your email to confirm that you received the message.

`NOTE:` Various other protocols (like http, sms, sqs) are supported by SNS. Follow [SNS documentation](http://docs.aws.amazon.com/AmazonCloudWatch/latest/DeveloperGuide/WhatIsCloudWatch.html) to setup subscriptions for other protocols.

#### Install jq to parse JSON response
AWS CLI returns command response in JSON format by default. You can pass the `--output` parameter to change the output format to text or a table. We use the default JSON output as it is structured and easier to parse than the other formats.

To parse JSON, we use the open source command-line JSON parser [jq](http://stedolan.github.io/jq/). Installation instructions for various platforms are here - http://stedolan.github.io/jq/download/

CPU Utilization Alarm
---------------------
CPU utilization metric is setup with default cloudwatch monitoring for an RDS instance. It is part of the **AWS/RDS** namespace.

Run the following command to create an alarm when CPU usage is greater than 75% on the **phixdemo** instance -
```bash
$ service_name=rds
$ instance_id=phixdemo
$ alarm_type=cpu-util
$ alarm_name="$alarm_type-$instance_id-$service_name"
$ aws cloudwatch put-metric-alarm --alarm-name $alarm_name --alarm-description "CPU utilization alarm for $instance_id RDS" --metric-name CPUUtilization --namespace AWS/RDS --statistic Average --period 300 --threshold 75 --comparison-operator GreaterThanThreshold  --dimensions Name=DBInstanceIdentifier,Value=$instance_id  --evaluation-periods 1 --alarm-actions arn:aws:sns:us-east-1:504357334963:phix-demo-alert-topic --unit Percent
```
#### Test the alarm
Change the state of the alarm to `OK` and then to `ALARM` -
```
$ aws cloudwatch set-alarm-state  --alarm-name cpu-util-phixdemo-rds --state-reason "testing" --state-value OK
$ aws cloudwatch set-alarm-state  --alarm-name cpu-util-phixdemo-rds --state-reason "testing" --state-value ALARM
```
This will send out a notification to the SNS topic which will send email to subscribers. Check if you received the email.

Reset the state of the alarm to `OK`.

Database Connections Usage Alarm
--------------------------
The `DatabaseConnections` metric in itself is not enough to monitor excessive database connections. The absolute number of active database connections that it returns has to be compared against the allowed number of maximum connections to calculate utilization. RDS does not post percentage utilization metrics. We can calculate percentage utilization by dividing active connections by the maximum limit.

AWS RDS command line interface allows us to query the maximum `processes` (Oracle) or `max_connections` (MySQL/PostgreSQL) parameter value. To be able to query these parameters, their values should be set in a custom parameter group that is different from the _engine-default_ provided by RDS. For example, the `processes` parameter value for the **phix** parameter group is set to 800.

### Oracle
We calculate database connections usage by comparing number of active connections to the allowed number of maximum `processes`. This is a good approximation as we leave the `sessions` parameter at it's _engine-default_ value set by RDS.

When the number of connections goes beyond 75% for 3 consecutive periods of 5 minutes, an alarm is raised.

Lookup database parameter group for instance -
```bash
$ db_param_group=$(aws rds describe-db-instances --db-instance-identifier $instance_id --query "DBInstances[0].DBParameterGroups[0].DBParameterGroupName" | sed -e 's/^"//' -e 's/"$//')
```
Get database parameters for the group -
```bash
$ params_json=$(aws rds describe-db-parameters --db-parameter-group-name $db_param_group)
```
Query maximum number of allowed processes -
```bash
$ max_processes=$(echo $params_json | jq '.Parameters[] | select(.ParameterName == "processes") | .ParameterValue' | sed -e 's/^"//' -e 's/"$//')
```
Retrieve current open connections -
```bash
$ start_time=$(date -v -5M +"%Y-%m-%dT%H:%M:%S")
$ end_time=$(date +"%Y-%m-%dT%H:%M:%S")
$ dbconns_metric=$(aws cloudwatch get-metric-statistics --namespace AWS/RDS --metric-name DatabaseConnections --dimensions Name=DBInstanceIdentifier,Value=$instance_id --start-time $start_time --end-time $end_time --period 300 --statistics Average)
$ dbconns_used=$(echo $dbconns_metric | jq 'select(.Label == "DatabaseConnections") | .Datapoints[0].Average')
```
Calculate utilization -
```bash
$ conns_utilization=$(echo "scale=2; $dbconns_used/$max_processes * 100" | bc)
```
Push custom metric to **Custom/RDS** namespace -
```bash
$ namespace="Custom/RDS"
$ metric_name="DatabaseConnectionsUtilization"
$ aws cloudwatch put-metric-data --namespace $namespace --metric-name $metric_name --unit Percent --value $conns_utilization --dimensions Name=DBInstanceIdentifier,Value=$instance_id
```
To schedule pushing this metric to Cloudwatch, create a `cron` entry for this script - https://git.demo.hcentive.com/techops/aws/push-rds-metrics. It takes a list of RDS `DBInstanceIdentifier` dimensions as a parameter.

Update cron -
```
$ sudo su -
$ crontab -e
```
Add the following entry to schedule metrics push every 5 minutes -
```bash
*/5 * * * * /opt/aws-cloudwatch/push-rds-metrics --instances-ids phixdemo
```

### MySQL

Binary Log Usage Alarm
----------------------
`NOTE: Applicable only for MySQL databases`

Any changes made to database structure or data are stored in binary log. The binary log also contains execution time for DML queries.

AWS provides metrics for absolute log usage in bytes. Not all RDS instances have the same amount of allocated storage. To measure usage consistently, we setup alarms on percentage of binary log utilization.
```bash
$ alarm_type=binlog-util
$ alarm_name="$alarm_type-$instance_id-$service_name"
$ aws cloudwatch put-metric-alarm --alarm-name $alarm_name --alarm-description "Binary Log usage alarm for $instance_id" --metric-name BinLogDiskUsage --namespace AWS/RDS --statistic Average --period 300 --threshold 75 --comparison-operator GreaterThanThreshold  --dimensions Name=DBInstanceIdentifier,Value=$instance_id  --evaluation-periods 1 --alarm-actions arn:aws:sns:us-east-1:504357334963:phix-demo-alert-topic --unit Bytes
```

References
----------
* [Cloudwatch command line API](http://docs.aws.amazon.com/cli/latest/reference/cloudwatch/index.html)
* [AWS Cloudwatch documentation](http://docs.aws.amazon.com/AmazonCloudWatch/latest/DeveloperGuide/WhatIsCloudWatch.html)
* [AWS RDS Dimensions and Metrics](http://docs.aws.amazon.com/AmazonCloudWatch/latest/DeveloperGuide/rds-metricscollected.html)
