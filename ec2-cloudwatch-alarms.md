Setting up Cloudwatch alarms for EC2 instances
===========================================
This document describes how to setup up [Cloudwatch](http://aws.amazon.com/cloudwatch/) alarms for EC2 instances.

AWS provides a number of instance metrics as part of their services. Run the `aws cloudwatch list-metrics` command to view available metics. For example, to view list of available EC2 metrics, run the following command -
```
$ aws cloudwatch list-metrics --namespace "AWS/EC2"
```
Apart from the provided metrics, Cloudwatch allows users to post custom metrics. An example of this are [custom cloudwatch monitoring scripts](http://docs.aws.amazon.com/AmazonCloudWatch/latest/DeveloperGuide/mon-scripts-perl.html) sample from AWS.
Design
------
[hCentive's](http://www.hcentive.com) EC2 instances post standard and custom metrics to Cloudwatch. Most instances post CPU, memory and disk utilization (root partition) metrics at 5 minute intervals. If the threshold for a metric is breached, the alarm's state will change from `OK` to `ALARM`. A notification is sent to a [SNS](http://aws.amazon.com/sns/) topic when this happens. Whoever is subsrcibed to this topic will receive the notification.
Setup
-----
The following steps need to be completed to setup up alarms for every set of servers - for example, demo servers for PHIX.

AWS command line tools should be installed on system used to setup these alarms.
#### Install jq to parse JSON response
AWS CLI returns command response in JSON format by default. You can pass the `--output` parameter to change the output format to text or a table. We use the default JSON output as it is structured and easier to parse than the other formats.

To parse JSON, we use the open source command-line JSON parser [jq](http://stedolan.github.io/jq/). Installation instructions for various platforms are here - http://stedolan.github.io/jq/download/

#### Create SNS topic
```json
$ topic_arn_json=$(aws sns create-topic --name phix-demo-alert-topic)
```
This will return the topic ARN (Amazon Resource Name) and set it as the value of `topic_arn_json`.
```json
{
    "TopicArn": "arn:aws:sns:us-east-1:504357334963:phix-demo-alert-topic"
}
```
Get the the ARN from the JSON response and set it in the `topic_arn` variable.
```bash
$ topic_arn=$(echo $topic_arn_json | jq '.TopicArn' | sed -e 's/^"//' -e 's/"$//')
```
#### Subscribe to topic
Subscribe email address to the topic, using the ARN above, to receive notifications.
```bash
$ aws sns subscribe --topic-arn $topic_arn --protocol email --notification-endpoint satyendra.sharma@hcentive.com
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
$ aws sns list-subscriptions-by-topic --topic-arn $topic_arn
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
```
$ aws sns publish --message "Verification" --topic $topic_arn
```
SNS will return a response like this -
```jason
{
    "MessageId": "42f189a0-3094-5cf6-8fd7-c2dde61a4d7d"
}
```
Check your email to confirm that you received the message.

`NOTE:` Various other protocols (like http, sms, sqs) are supported by SNS. Follow [SNS documentation](http://docs.aws.amazon.com/AmazonCloudWatch/latest/DeveloperGuide/WhatIsCloudWatch.html) to setup subscriptions for other protocols.

CPU Utilization Alarm
---------------------
CPU utilization metric is setup with default cloudwatch monitoring for an EC2 instance. It is part of the **AWS/EC2** namespace.

Run the following commands to create an alarm when CPU usage is greater than 75% on the PHIX-DM02 instance (i-7fb30e17) -
```bash
$ instance_id=i-7fb30e17
$ resp=$(aws ec2 describe-instances --instance-ids $instance_id)
$ instance_name=$(echo $resp | jq '.Reservations[0].Instances[0].Tags[] | select(.Key == "Name") | .Value' | sed -e 's/^"//' -e 's/"$//')
$ if [ -z $instance_name ]; then instance_name=$instance_id; fi
$ alarm_name="cpu-util-$instance_name"
$ aws cloudwatch put-metric-alarm --alarm-name $alarm_name --alarm-description "CPU utilization alarm for $instance_name" --metric-name CPUUtilization --namespace AWS/EC2 --statistic Average --period 300 --threshold 75 --comparison-operator GreaterThanThreshold  --dimensions Name=InstanceId,Value=$instance_id  --evaluation-periods 1 --alarm-actions $topic_arn --unit Percent
```
#### Test the alarm
Change the state of the alarm to `OK` and then to `ALARM` -
```bash
$ aws cloudwatch set-alarm-state  --alarm-name cpu-util-i-7fb30e17 --state-reason "testing" --state-value OK
$ aws cloudwatch set-alarm-state  --alarm-name cpu-util-i-7fb30e17 --state-reason "testing" --state-value ALARM
```
This will send out a notification to the SNS topic which will send email to subscribers. Check if you received the email.

Reset the state of the alarm to `OK`.

Memory and Disk Utilization Alarms
----------------------------------
AWS does not provide metrics for memory and disk utilization as part of the default cloudwatch offering. But it does provide a way to push custom metrics to cloudwatch.

We use the sample monitoring perl scripts that AWS has made available for download to push memory and disk utilization metrics to cloudwatch.

#### Setup
For existing instances, create an IAM group with permissions for cloudwatch and sns actions.
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

#### Installation
You will need perl and ssl installed on the servers
```bash
$ sudo apt-get install unzip libwww-perl libcrypt-ssleay-perl
```
Download and unzip the custom metric scripts on the server you want to push metrics from - like the PHIX demo servers
```bash
$ wget http://ec2-downloads.s3.amazonaws.com/cloudwatch-samples/CloudWatchMonitoringScripts-v1.1.0.zip
$ sudo unzip CloudWatchMonitoringScripts-v1.1.0.zip -d /opt
$ rm CloudWatchMonitoringScripts-v1.1.0.zip
$ cd /opt/aws-scripts-mon
```
Rename the awscred.template file to awscreds.conf. Enter the access and secret keys created in the setup section in this file. Change permissions of the file to read only by owner.

Test the script by running the following command
```bash
$ ./mon-put-instance-data.pl --mem-util -disk-space-util --disk-path=/ --verify --verbose
```
Schedule sending metrics using a cron job.
```bash
$ sudo su -
$ crontab -e
```
Enter the following at the end of the file
```bash
*/5 * * * * /opt/aws-scripts-mon/mon-put-instance-data.pl --aws-credential-file=/opt/aws-scripts-mon/awscreds.conf --mem-util --disk-space-util --disk-path=/ --from-cron
```
The server will now push memory and disk utilization metrics to cloudwatch under the **System/Linux** namespace.

#### Create Alarms
Create memory utilization alarm for an instance that will send a notification to the SNS topic when utilized memory on the server crosses 75%
```bash
$ aws cloudwatch put-metric-alarm --alarm-name mem-util-i-7fb30e17 --alarm-description "Memory utilization alarm for i-7fb30e17" --metric-name MemoryUtilization --namespace System/Linux --statistic Average --period 300 --threshold 75 --comparison-operator GreaterThanThreshold  --dimensions Name=InstanceId,Value=i-7fb30e17  --evaluation-periods 1 --alarm-actions $topic_arn --unit Percent
```
Create disk space utilization alarm for an instance that will send a notification to the SNS topic when disk utilization of the root partition ("/") goes above 75%
```bash
$ aws cloudwatch put-metric-alarm --alarm-name disk-util-i-7fb30e17 --alarm-description "Disk utilization alarm for i-7fb30e17" --metric-name DiskSpaceUtilization --namespace System/Linux --statistic Average --period 300 --threshold 75 --comparison-operator GreaterThanThreshold  --dimensions Name=InstanceId,Value=i-7fb30e17  --evaluation-periods 1 --alarm-actions $topic_arn --unit Percent
```

It is possible to push custom metrics to cloudwatch using the cloudwatch API. Web servers can push apache and tomcat/jetty metrics. Similarly, application servers can push database connectivity metrics.

References
----------
* [Cloudwatch command line API](http://docs.aws.amazon.com/cli/latest/reference/cloudwatch/index.html)
* [AWS Cloudwatch documentation](http://docs.aws.amazon.com/AmazonCloudWatch/latest/DeveloperGuide/WhatIsCloudWatch.html)
* [AWS Cloudwatch Monitoring Scripts](https://github.com/sanojimaru/amazon-cloudwatch-monitoring-scripts)
