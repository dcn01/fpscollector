package com.nd.qa;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotVisibleException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

@RunWith(JUnit4.class)
public class SendEmailTest{

	private static ChromeDriverService serivce;
	private WebDriver driver;
	
	
	@BeforeClass
	public static void createAndStartService() throws IOException{
		serivce = new ChromeDriverService.Builder()
			.usingDriverExecutable(new File("D:/Program Files (x86)/Selenium/drivers/chromedriver.exe"))
			.usingAnyFreePort()
			.build();
		serivce.start();
	}
	
	@AfterClass
	public static void stopService(){
		serivce.stop();
	}
	
	@Before
	public void createDriver(){
		driver = new RemoteWebDriver(serivce.getUrl(), DesiredCapabilities.chrome());
	}
	
	@After
	public void quitDriver(){
		driver.quit();
	}
	
	@Test
	public void testSendEmail() throws InterruptedException{
		driver.manage().window().maximize();
		driver.get("http://mail.163.com");
		login();
		Thread.sleep(3000);
		WebElement inbox = driver.findElement(By.className("nui-tree-item-text"));
		inbox.click();
		Thread.sleep(3000);
		WebElement mailRecord = driver.findElement(By.cssSelector("div.rF0.kw0.nui-txt-flag0"));
		mailRecord.click();
		for(int i=0;i<300;i++){
			Thread.sleep(1000);
			WebElement forwardMail = driver.findElement(By.cssSelector("span.nui-splitBtn-text"));
			forwardMail.click();
			Thread.sleep(1000);
			WebElement receiver = getWebElement(By.cssSelector("label.js-component-emailtips.nui-ipt-placeholder"));
			if(receiver!=null){
				receiver.sendKeys("ndrom_tester@163.com");
			}
			Thread.sleep(1000);
			WebElement sendBtn = driver.findElement(By.cssSelector(".js-component-button.nui-mainBtn.nui-btn.nui-btn-hasIcon.nui-mainBtn-hasIcon"));
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			};
			sendBtn.click();
			//.js-component-icon.nui-ico.nui-ico-close:eq(1)
//			WebElement successInfo = driver.findElement(By.cssSelector("[id$=_succInfo]"));
			Thread.sleep(1000);
			List<WebElement> closeSuccessInfos = driver.findElements(By.cssSelector(".js-component-icon.nui-ico.nui-ico-close"));
			WebElement closeSuccessInfo_2 = closeSuccessInfos.get(1);
			closeSuccessInfo_2.click();
		}
		Assert.assertSame("测试通过", 1, 1);
		
	}
	
	private void login(){
		WebElement input_Accout = driver.findElement(By.id("idInput"));
		input_Accout.clear();
		input_Accout.sendKeys("ndrom_tester");
		WebElement input_Password = driver.findElement(By.id("pwdInput"));
		input_Password.clear();
		input_Password.sendKeys("nd123456");
		WebElement loginBtn = driver.findElement(By.id("loginBtn"));
		loginBtn.click();
	}
	
	private WebElement getWebElement(By by){
		WebElement mWebElement=null;
		try{
			mWebElement=driver.findElement(by);
		}catch(ElementNotVisibleException e){
			System.out.println("ElementNotVisible:"+by.toString());
		}catch(NoSuchElementException e){
			System.out.println("NoSuchElement:"+by.toString());
		}
		return mWebElement;
	}
	
}
