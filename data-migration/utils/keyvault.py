import os
import cmd
from azure.keyvault.secrets import SecretClient
from azure.identity import DefaultAzureCredential

def get_secret(kv_uri, secret_name):
  credential = DefaultAzureCredential()
  client = SecretClient(vault_url=kv_uri, credential=credential)
  secret = client.get_secret(secret_name)
  return secret.value