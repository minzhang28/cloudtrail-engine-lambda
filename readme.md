## CloudTrail Engine Lambda
A lambda function (rule engine) to monitor AWS api calls base on the CloudTrail event.


#### Background
In AWS, it's easy to maintain the `infra as code` by using existing services like `terraform`
or `CloudFormation`, but there's no monitoring of the real time changes happened on the infra, which leaves the OPS people blind
on the specious activities on the AWS environment. This to provide a `near real time` alerting API service to track and notify any unexpected API calls happened on the AWS environment.

#### Why Rule Engine
AWS provides CloudWatch or AWS Config service to monitor the API calls to the AWS account but they have their own disadvantages:
- CloudWatch: Not that flexible on defining the rules to trigger Alerts
- AWS Config: Not able to provide real time alerts (periodically checking the changes)

#### What does CloudTrail Engine do
CloudTrail engine provides a much easier way to define the rules that you want to trigger the alerts base on the CloudTrail event, all you need to do is to make changes to the rules files (.drl) without making any changes of code.


#### Software Stack
- [Drools](https://www.drools.org/) : Rule engine to analytics rules


#### Diagram
![Diagram](diagram.png)
- When you create the Lambda function for CloudWatch Events, you can create an event selector, which functions as a filter for the Lambda function so that the function is only invoked for specific events. This approach has two advantages: it avoids unnecessary calls to Lambda
- For the example below, I am using this rule selector to filter all the changes of security group rules to trigger the CloudTrail engine lambda function  
```
{
  "detail-type": [
    "AWS API Call via CloudTrail"
  ],
  "detail": {
    "eventSource": [
      "ec2.amazonaws.com"
    ],
    "eventName": [
      "AuthorizeSecurityGroupIngress",
      "RevokeSecurityGroupIngress"
    ]
  }
}
```


#### CloudTrail Event Example
CloudTrail engine flatten the CloudTrail event into a flat JSON, blow is an example of user `user@company.com` made changes to security group `sg-abcdefg` from AWS web console to allow all traffic from IP `1.2.3.4/32`. For more details about the CloudTrail event please refer to [this aws doc](https://docs.aws.amazon.com/awscloudtrail/latest/userguide/cloudtrail-event-reference-record-contents.html)

```
{
  "eventID": "d3510c1d-2f67-45f1-b145-db0b7190202a",
  "awsRegion": "us-east-2",
  "eventVersion": "1.05",
  "responseElements.requestId": "f723aa75-d771-4dff-97d1-6b81e663ff8c",
  "responseElements._return": true,
  "sourceIPAddress": "1.2.3.4",
  "eventSource": "ec2.amazonaws.com",
  "requestParameters.groupId": "sg-abcdefg",
  "requestParameters.ipPermissions.items[0].ipProtocol": "tcp",
  "requestParameters.ipPermissions.items[0].fromPort": 0,
  "requestParameters.ipPermissions.items[0].toPort": 0,
  "requestParameters.ipPermissions.items[0].groups": {

  },
  "requestParameters.ipPermissions.items[0].ipRanges.items[0].cidrIp": "1.2.3.4/32",
  "requestParameters.ipPermissions.items[0].ipRanges.items[0].description": "test",
  "requestParameters.ipPermissions.items[0].ipv6Ranges": {

  },
  "requestParameters.ipPermissions.items[0].prefixListIds": {

  },
  "userAgent": "console.ec2.amazonaws.com",
  "userIdentity.type": "AssumedRole",
  "userIdentity.principalId": "AROAIIEUYW2FCCWM6LZJ2:user@company.com",
  "userIdentity.arn": "arn:aws:sts::123456789012:assumed-role/assume-role-name/user@company.com",
  "userIdentity.accountId": "123456789012",
  "userIdentity.accessKeyId": "ASIATA5XIF7AHCRZG3NC",
  "userIdentity.sessionContext.attributes.mfaAuthenticated": "true",
  "userIdentity.sessionContext.attributes.creationDate": "2018-08-22T01:27:16Z",
  "userIdentity.sessionContext.sessionIssuer.type": "Role",
  "userIdentity.sessionContext.sessionIssuer.principalId": "AROAIIEUYW2FCCWM6LZJ2",
  "userIdentity.sessionContext.sessionIssuer.arn": "arn:aws:iam::123456789012:role/assume-role-name",
  "userIdentity.sessionContext.sessionIssuer.accountId": "123456789012",
  "userIdentity.sessionContext.sessionIssuer.userName": "assume-role-name",
  "eventType": "AwsApiCall",
  "requestID": "f723aa75-d771-4dff-97d1-6b81e663ff8c",
  "eventTime": "2018-08-22T02:08:03Z",
  "eventName": "AuthorizeSecurityGroupIngress"
}
```
The reason to why I flattern this JSON is to provide an easier way to define the rules. see the `Rules` section below

#### Cloud Engine Rules
Cloud Engine rule is customized rules that yield `true` or `false` in order to trigger responsive actions from downstream. It's
using [Drools Rule Lanauage](https://docs.jboss.org/drools/release/7.9.0.Final/drools-docs/html_single/index.html#_droolslanguagereferencechapter).

This example rule filters the CloudTrail event and will be triggered if
- Someone (user@company.com) manually change any security ingress rules from AWS console

```
import java.util.Map
import cloudengine.AlertServices.DatadogAlertService;


rule "ec2.amazonaws.com: Test Alert - Someone is Changing security group"
       	when
       		$map: Map(this["userIdentity.arn"].contains("user@company.com") &&
       		          this["eventName"] == "AuthorizeSecurityGroupIngress" &&
       		          this["userAgent"].contains("console"))
       	then
       	    new DatadogAlertService().report($map, drools.getRule().getName());
end
```

#### Cloud Engine Rule Naming Convention
The lambda function reads the input value of `eventSource` field and using it's value to find the rules defined in the rules folder.

In order to support multiple AWS rules, please name the rules with "$eventSource: $ruleName", for example:
- `ec2.amazonaws.com: someone changes the security group`
- `ec2.amazonaws.com: someone terminates EC2`
- `s3.amazonaws.com: someone made the bucket publicly accessabe`

#### Deploy and run
- `mvn package` to package the jar file
- Create a lambda function on AWS with `java8` running environment
- Config the lambda handler as `cloudengine.LambdaHandler::handleRequest`
