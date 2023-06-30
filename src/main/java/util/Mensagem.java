package util;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Mensagem implements Serializable {
  private static final long serialVersionUID = 1L;

  private String operacao;
  private Status status;
  private Map<String, Object> params;

  public Mensagem() {
    this.params = new HashMap<>();
  }

  public Mensagem(String operacao) {
    this.operacao = operacao;
    this.status = Status.OK; // Default status is OK
    this.params = new HashMap<>();
  }

  public String getOperacao() {
    return operacao;
  }

  public void setOperacao(String operacao) {
    this.operacao = operacao;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public void setParam(String key, Object value) {
    params.put(key, value);
  }

  public Object getParam(String key) {
    return params.get(key);
  }

  public Set<String> getParamKeys() {
    return params.keySet();
  }

  public boolean containsParam(String key) {
    return params.containsKey(key);
  }

  public void removeParam(String key) {
    params.remove(key);
  }
}