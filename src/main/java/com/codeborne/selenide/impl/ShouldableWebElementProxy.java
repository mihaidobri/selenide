package com.codeborne.selenide.impl;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ShouldableWebElement;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URISyntaxException;
import java.net.URL;

import static com.codeborne.selenide.DOM.assertElement;
import static com.codeborne.selenide.WebDriverRunner.fail;

public class ShouldableWebElementProxy implements InvocationHandler {
  public static ShouldableWebElement wrap(WebElement element) {
    return (element instanceof ShouldableWebElement) ?
        (ShouldableWebElement) element :
        (ShouldableWebElement) Proxy.newProxyInstance(
            element.getClass().getClassLoader(), new Class<?>[]{ShouldableWebElement.class}, new ShouldableWebElementProxy(element));
  }

  private final WebElement delegate;

  private ShouldableWebElementProxy(WebElement delegate) {
    this.delegate = delegate;
  }

  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    if ("should".equals(method.getName()) || "shouldHave".equals(method.getName()) || "shouldBe".equals(method.getName())) {
      return should(proxy, (Condition[]) args[0]);
    }
    if ("shouldNot".equals(method.getName()) || "shouldNotHave".equals(method.getName()) || "shouldNotBe".equals(method.getName())) {
      return shouldNot(proxy, (Condition[]) args[0]);
    }
    if ("find".equals(method.getName())) {
      return wrap(find(args[0]));
    }
    if ("toString".equals(method.getName())) {
      return describe(delegate);
    }
    if ("uploadFromClasspath".equals(method.getName())) {
      return uploadFromClasspath((String) args[0]);
    }

    return delegateMethod(method, args);
  }

  private String describe(WebElement element) {
    return new Describe(element)
        .attr("id").attr("name").attr("class").attr("value").attr("disabled").attr("type").attr("placeholder")
        .attr("onclick").attr("onClick").attr("onchange").attr("onChange")
        .toString();
  }

  private Object uploadFromClasspath(String fileName) throws URISyntaxException {
    if (!"input".equalsIgnoreCase(delegate.getTagName())) {
      throw new IllegalArgumentException("Cannot upload file because " + describe(delegate) + " is not an INPUT");
    }

    URL resource = Thread.currentThread().getContextClassLoader().getResource(fileName);
    if (resource == null) {
      throw new IllegalArgumentException("File not found in classpath: " + fileName);
    }
    File file = new File(resource.toURI());
    delegate.sendKeys(file.getAbsolutePath());
    return file;
  }

  private Object should(Object proxy, Condition[] conditions) {
    for (Condition condition : conditions) {
      assertElement(delegate, condition);
    }
    return proxy;
  }

  private Object shouldNot(Object proxy, Condition[] conditions) {
    for (Condition condition : conditions) {
      if (condition.apply(delegate)) {
        fail("Element " + delegate.getTagName() + " has " + condition);
      }
    }
    return proxy;
  }

  private WebElement find(Object arg) {
    return (arg instanceof By) ?
        delegate.findElement((By) arg) :
        delegate.findElement(By.cssSelector((String) arg));
  }

  private Object delegateMethod(Method method, Object[] args) throws Throwable {
    try {
      return method.invoke(delegate, args);
    } catch (InvocationTargetException e) {
      throw e.getTargetException();
    }
  }
}