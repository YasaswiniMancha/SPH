{{/* Common helpers for SmartPayHub chart */}}
{{- define "smartpayhub.fullname" -}}
{{- printf "%s-%s" .Release.Name .Values.service.name | trunc 63 | trimSuffix "-" -}}
{{- end -}}

