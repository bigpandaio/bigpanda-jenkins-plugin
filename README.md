[![Overall](https://img.shields.io/endpoint?style=flat&url=https%3A%2F%2Fapp.opslevel.com%2Fapi%2Fservice_level%2FJALVv3KIE0w3-kFVsWtwNRMQEyK7AU22s5VKGxqb6B0)](https://app.opslevel.com/services/bigpanda-jenkins-plugin/maturity-report)
[![Service Ownership](https://img.shields.io/endpoint?style=flat&url=https%3A%2F%2Fapp.opslevel.com%2Fapi%2Fservice_level%2FJALVv3KIE0w3-kFVsWtwNRMQEyK7AU22s5VKGxqb6B0%2Fservice_ownership)](https://app.opslevel.com/services/bigpanda-jenkins-plugin/maturity-report)
[![Security](https://img.shields.io/endpoint?style=flat&url=https%3A%2F%2Fapp.opslevel.com%2Fapi%2Fservice_level%2FJALVv3KIE0w3-kFVsWtwNRMQEyK7AU22s5VKGxqb6B0%2Fsecurity)](https://app.opslevel.com/services/bigpanda-jenkins-plugin/maturity-report)
[![Reliability](https://img.shields.io/endpoint?style=flat&url=https%3A%2F%2Fapp.opslevel.com%2Fapi%2Fservice_level%2FJALVv3KIE0w3-kFVsWtwNRMQEyK7AU22s5VKGxqb6B0%2Freliability)](https://app.opslevel.com/services/bigpanda-jenkins-plugin/maturity-report)
[![Performance](https://img.shields.io/endpoint?style=flat&url=https%3A%2F%2Fapp.opslevel.com%2Fapi%2Fservice_level%2FJALVv3KIE0w3-kFVsWtwNRMQEyK7AU22s5VKGxqb6B0%2Fperformance)](https://app.opslevel.com/services/bigpanda-jenkins-plugin/maturity-report)
[![Scalability](https://img.shields.io/endpoint?style=flat&url=https%3A%2F%2Fapp.opslevel.com%2Fapi%2Fservice_level%2FJALVv3KIE0w3-kFVsWtwNRMQEyK7AU22s5VKGxqb6B0%2Fscalability)](https://app.opslevel.com/services/bigpanda-jenkins-plugin/maturity-report)
[![Observability](https://img.shields.io/endpoint?style=flat&url=https%3A%2F%2Fapp.opslevel.com%2Fapi%2Fservice_level%2FJALVv3KIE0w3-kFVsWtwNRMQEyK7AU22s5VKGxqb6B0%2Fobservability)](https://app.opslevel.com/services/bigpanda-jenkins-plugin/maturity-report)
[![Infrastructure](https://img.shields.io/endpoint?style=flat&url=https%3A%2F%2Fapp.opslevel.com%2Fapi%2Fservice_level%2FJALVv3KIE0w3-kFVsWtwNRMQEyK7AU22s5VKGxqb6B0%2Finfrastructure)](https://app.opslevel.com/services/bigpanda-jenkins-plugin/maturity-report)
[![Quality](https://img.shields.io/endpoint?style=flat&url=https%3A%2F%2Fapp.opslevel.com%2Fapi%2Fservice_level%2FJALVv3KIE0w3-kFVsWtwNRMQEyK7AU22s5VKGxqb6B0%2Fquality)](https://app.opslevel.com/services/bigpanda-jenkins-plugin/maturity-report)
[![Resiliency](https://img.shields.io/endpoint?style=flat&url=https%3A%2F%2Fapp.opslevel.com%2Fapi%2Fservice_level%2FJALVv3KIE0w3-kFVsWtwNRMQEyK7AU22s5VKGxqb6B0%2Fresiliency)](https://app.opslevel.com/services/bigpanda-jenkins-plugin/maturity-report)

# BigPanda Global Notification Plugin

## How It Works
The BigPanda global notifier hooks into the Jenkins build process, triggering the Jenkins instance on every build. Once the build starts, the active change is sent to BigPanda and becomes available for correlation with incidents. When the build is finished, the change data in BigPanda is updated to reflect the completed change. See [the docs](https://docs.bigpanda.io/docs/jenkins) for more information.

## Installing the Plugin
1. Log into your Jenkins instance.
2. Go to Manage Jenkins > Manage Plugins on the left panel.
3. Select the Available tab.
4. Search for the BigPanda Notifier and click Install.
5. Allow your Jenkins instance to restart.

## Setup
1. Go to Manage Jenkins > Configure System.
2. In the Options, find the BigPanda Notifier.
3. Add the [API Key](https://docs.bigpanda.io/docs/api-keys-management) and [App key](https://a.bigpanda.io/#/app/integrations/jenkins/instructions/jenkins) for this plugin.
4. Apply and Save your changes.