package com.app.common.okgo.callback;

public abstract class EntityCallback<T>{

    public abstract void onSuccess(T entity);

    public abstract void onFail(Exception e);


}
