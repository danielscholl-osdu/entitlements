from msrestazure.azure_active_directory import AADTokenCredentials
import adal


def generate_service_principal_token(service_principal_id, service_principal_secret, azure_tenant_id, resource_id):
  authority_host_uri = 'https://login.microsoftonline.com'
  authority_uri = authority_host_uri + '/' + azure_tenant_id
  context = adal.AuthenticationContext(authority_uri, api_version=None)
  token_response = context.acquire_token_with_client_credentials(resource_id, service_principal_id, service_principal_secret)
  return token_response['accessToken']