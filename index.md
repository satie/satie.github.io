I work at [hCentive](https://www.hcentive.com) as a technology manager, and contribute design and code for infrastructure maintenance.
I am currently working on [AWS WAF](https://aws.amazon.com/waf/) and [AWS KMS](https://aws.amazon.com/kms/).

Some of my active projects for WAF are -
* An [AWS Lambda function](https://github.com/hcentive/waf-update-ipdatabase) to create and update a database of blacklisted IP addresses.
* An [AWS Lambda function](https://github.com/hcentive/waf-update-blacklist) to create and update [WAF IPSets](http://docs.aws.amazon.com/waf/latest/APIReference/API_IPSet.html) from a [database of blacklisted IP addresses](https://github.com/hcentive/waf-update-ipdatabase).
* An [AWS Lambda function](https://github.com/hcentive/waf-update-badbotdata) to create and update [AWS WAF string match conditions](http://docs.aws.amazon.com/waf/latest/developerguide/web-acl-string-conditions.html) to reject requests from know malicious bots.

Some work on KMS -
* [Design](https://github.com/hcentive/kms) for AWS Key Management System (KMS) implementation for hCentive
* A [Ruby gem](https://github.com/hcentive/hcentive-kms-cli) for hCentive's KMS implementation

Cloud resources management tools -
* [Design](https://github.com/hcentive/cloudmanage) for hCentive's cloud resource management tool
* [EC2 instance startup and shutdown tool](https://github.com/hcentive/ec2-startup-shutdown) based on start and stop times defined in instance tags
* [Nagios plugin](https://github.com/hcentive/socketlabs-status) for [Socketlabs](https://www.socketlabs.com) SMTP server usage metrics
* [NodeJS module](https://github.com/hcentive/brocade-update-firewall) to update [Brocade VTM](http://www.brocade.com/en/products-services/software-networking/application-delivery-controllers/virtual-traffic-manager.html) security protection class and rules

Miscellaneous howtos and guides -
* [Recommended SSL configuration for web applications](how-to-ssllabs.md)
* [Set up Cloudwatch alarms for RDS instances](rds-cloudwatch-alarms.md)
* [dm-crypt performance on CentOS 6 and CentOS 7](https://github.com/satie/dm-crypt-centos-performance)
* [Git setup at hCentive](https://github.com/hcentive/git-setup)
* [Sending email through Socketlabs](sending_email_through_socketlabs.md)
