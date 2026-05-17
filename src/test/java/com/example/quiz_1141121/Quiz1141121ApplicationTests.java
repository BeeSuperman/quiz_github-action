package com.example.quiz_1141121;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.example.quiz_1141121.service.QuizService;

import jakarta.validation.constraints.NotBlank;
@TestInstance(Lifecycle.PER_CLASS)
//測試檔案，主要還是測試dao，service的東西
@SpringBootTest
/*解釋:
它會模擬整個專案跑起來的樣子，
自動掃描所有的 @Service、@Component、@Repository 等 Bean，
並把它們載入到記憶體中，讓你可以在測試環境中使用 Spring 的功能（例如依賴注入）。這樣的@Autowired才有東西
執行test之前,做完的東西再執行test
*/
class Quiz1141121ApplicationTests {
//	@Autowired
//	private QuizService quizService;
	
	
	
	//這是 JUnit 5（測試框架）提供的註解
	//JUnit 只會執行有標註 @Test 的方法。
	@Test
	void contextLoads() {//方法的權限不能是private
		
		
		//Assert.isTrue()的作用？？？
		
		/*req是從controller檢查，這裡的測試檢查是從service進去，我們有用@NotBlank（在Creatreq)
		來檢查參數，這是一個Bug,要打開checkparam才可以
		舉例：
		測試title沒資料：
		Assert.isTrue(res.getMessage().equals(ReplyMessage.TITLE_ERROR.getMessage()),"測試title沒資料錯誤");
		解釋：這行程式碼使用 Assert.isTrue 進行單元測試驗證，檢查 res.getMessage() 是否等於預期的錯誤訊息。
         如果兩者不相等，測試就會失敗並顯示「測試title沒資料錯誤」這個提示。
          簡單說就是確認回傳的錯誤訊息與預設的標題錯誤訊息一致，確保程式在 title 沒資料時正確拋出預期錯誤。
		*/
		/*
		 * 測試其他邏輯。。。一直測試，。。。最後是測試正確的資料
		 * Assert.isTrue(res.getMessage().equals(ReplyMessage.SUCCESS.getMessage()),"測試是否success");
		 */
		
		}
	/* Befor: 用來創建測試資料
	 * All: 創建初始的測試資料
	 * Each: 根據每個方法要測試的內容，修改測試資料*/
	@BeforeEach
	public void beforeEachTest() {
		System.out.println("===== Before Each =====");
	}
	
	@BeforeAll
	public void beforeAllTest() {
		System.out.println("===== Before All =====");
	}
	
	/* After: 刪除測試用的資料
	 * All: 最後刪除所有的測試資料
	 * Each: 可以在每一次的測試之前，將有修改的測試資料再一次初始化*/
	@AfterEach
	public void afterEachTest() {
		System.out.println("===== After Each =====");
	}
	
	@AfterAll
	public void afterAllTest() {
		System.out.println("===== After All =====");
	}

}
