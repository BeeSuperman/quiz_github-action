package com.example.quiz_1141121.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.quiz_1141121.constants.ReplyMessage;
import com.example.quiz_1141121.dao.QuestionDao;
import com.example.quiz_1141121.dao.QuizDao;
import com.example.quiz_1141121.entity.Question;
import com.example.quiz_1141121.entity.Quiz;
import com.example.quiz_1141121.req.CreateReq;
import com.example.quiz_1141121.req.DeleteReq;
import com.example.quiz_1141121.res.BasicRes;
import com.example.quiz_1141121.res.CreateRes;
import com.example.quiz_1141121.res.GetQuizRes;
import com.example.quiz_1141121.res.GetSingleQuizRes;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

    @Mock
    private QuizDao quizDao;

    @Mock
    private QuestionDao questionDao;

    @InjectMocks
    private QuizService quizService;

    private CreateReq validReq;

    @BeforeEach
    void setUp() {
        // 準備一個合法的 CreateReq，每個測試開始前都重置
        validReq = new CreateReq();
        validReq.setTitle("測試問卷");
        validReq.setDescription("這是測試描述");
        validReq.setStartDate(LocalDate.now().plusDays(1));
        validReq.setEndDate(LocalDate.now().plusDays(10));
        validReq.setPublished(false);

        Question q = new Question();
        q.setQuestionId(1);
        q.setQuestion("你喜歡什麼？");
        q.setType("TEXT");
        q.setRequired(true);
        validReq.setQuestionList(List.of(q));
    }

    // ============================
    // create() 測試
    // ============================

    @Test
    void create_whenTitleIsEmpty_shouldReturnTitleError() {
        validReq.setTitle("");

        CreateRes res = quizService.create(validReq);

        assertEquals(ReplyMessage.TITLE_ERROR.getCode(), res.getCode());
        assertEquals(ReplyMessage.TITLE_ERROR.getMessage(), res.getMessage());
    }

    @Test
    void create_whenDescriptionIsEmpty_shouldReturnDescriptionError() {
        validReq.setDescription("");

        CreateRes res = quizService.create(validReq);

        assertEquals(ReplyMessage.DESCRIPTION_ERROR.getCode(), res.getCode());
        assertEquals(ReplyMessage.DESCRIPTION_ERROR.getMessage(), res.getMessage());
    }

    @Test
    void create_whenStartDateIsNull_shouldReturnStartDateError() {
        validReq.setStartDate(null);

        CreateRes res = quizService.create(validReq);

        assertEquals(ReplyMessage.START_DATE_ERROR.getCode(), res.getCode());
    }

    @Test
    void create_whenStartDateIsBeforeToday_shouldReturnStartDateError() {
        validReq.setStartDate(LocalDate.now().minusDays(1));

        CreateRes res = quizService.create(validReq);

        assertEquals(ReplyMessage.START_DATE_ERROR.getCode(), res.getCode());
    }

    @Test
    void create_whenEndDateIsNull_shouldThrowException() {
        validReq.setEndDate(null);

        // Bug：endDate null 時，checkParams 裡 startDate.isAfter(endDate) 會丟 NullPointerException
        // 根本原因：endDate 的 null 檢查在 startDate.isAfter(endDate) 之後，順序錯誤
        org.junit.jupiter.api.Assertions.assertThrows(
            NullPointerException.class,
            () -> quizService.create(validReq)
        );
    }

    @Test
    void create_whenAllParamsValid_shouldReturnSuccess() {
        when(quizDao.getMaxId()).thenReturn(1);

        CreateRes res = quizService.create(validReq);

        assertEquals(ReplyMessage.SUCCESS.getCode(), res.getCode());
        assertEquals(ReplyMessage.SUCCESS.getMessage(), res.getMessage());
    }

    // ============================
    // getQuizList() 測試
    // ============================

    @Test
    void getQuizList_shouldReturnSuccessWithList() {
        Quiz quiz = new Quiz();
        quiz.setId(1);
        quiz.setTitle("問卷一");
        when(quizDao.getAll()).thenReturn(List.of(quiz));

        GetQuizRes res = quizService.getQuizList();

        assertEquals(ReplyMessage.SUCCESS.getCode(), res.getCode());
        assertNotNull(res.getQuizList());
        assertEquals(1, res.getQuizList().size());
    }

    @Test
    void getQuizList_whenNoData_shouldReturnEmptyList() {
        when(quizDao.getAll()).thenReturn(List.of());

        GetQuizRes res = quizService.getQuizList();

        assertEquals(ReplyMessage.SUCCESS.getCode(), res.getCode());
        assertEquals(0, res.getQuizList().size());
    }

    // ============================
    // getQuiz() 測試
    // ============================

    @Test
    void getQuiz_whenQuizNotFound_shouldReturnNotFound() {
        when(quizDao.getById(999)).thenReturn(null);

        GetSingleQuizRes res = quizService.getQuiz(999);

        assertEquals(ReplyMessage.QUIZ_NOT_FOUND.getCode(), res.getCode());
    }

    @Test
    void getQuiz_whenQuizExists_shouldReturnSuccess() {
        Quiz quiz = new Quiz();
        quiz.setId(1);
        quiz.setTitle("問卷一");
        when(quizDao.getById(1)).thenReturn(quiz);
        when(questionDao.getByQuizId(1)).thenReturn(List.of());

        GetSingleQuizRes res = quizService.getQuiz(1);

        assertEquals(ReplyMessage.SUCCESS.getCode(), res.getCode());
    }

    @Test
    void getQuiz_whenQuizIdIsZero_shouldReturnNotFound() {
        // ===== Arrange（準備）=====
        when(quizDao.getById(0)).thenReturn(null);
        // 設定 Mock 行為：當有人呼叫 quizDao.getById(0) 時，回傳 null
        // 原因：資料庫裡不存在 ID=0 的問卷（資料庫 ID 從 1 開始）
        // 注意：這裡不是真的去查資料庫，而是告訴假 DAO 碰到 0 就回傳 null

        // ===== Act（執行）=====
        GetSingleQuizRes res = quizService.getQuiz(0);
        // 呼叫真實的 QuizService.getQuiz()，傳入 quizId=0
        // QuizService 會拿著 0 去問假 DAO，假 DAO 回傳 null
        // QuizService 看到 null 就應該回傳 QUIZ_NOT_FOUND

        // ===== Assert（驗證）=====
        assertEquals(ReplyMessage.QUIZ_NOT_FOUND.getCode(), res.getCode());
        // 驗證：回傳的 code 是否是 404（QUIZ_NOT_FOUND）
        // 如果 QuizService 沒有正確處理 null，這行就會失敗
    }

    @Test
    void getQuiz_whenQuizIdIsNegative_shouldReturnNotFound() {
        // ===== Arrange =====
        when(quizDao.getById(-1)).thenReturn(null);
        // 設定 Mock：查詢 ID=-1 時回傳 null
        // 負數 ID 在資料庫裡不存在

        // ===== Act =====
        GetSingleQuizRes res = quizService.getQuiz(-1);

        // ===== Assert =====
        assertEquals(ReplyMessage.QUIZ_NOT_FOUND.getCode(), res.getCode());
        // 驗證：負數 ID 也應該回傳 NOT_FOUND
        // 這個測試同時揭露一個設計問題：
        // QuizService.getQuiz() 沒有在一開始就擋掉 quizId <= 0 的情況
        // 而是讓它去查資料庫，查不到才回傳 NOT_FOUND
        // 業界更好的做法是：一進來就檢查 quizId > 0，不合法直接回錯誤
    }

    // ============================
    // delete(DeleteReq) 測試
    // ============================

    @Test
    void delete_whenQuizIdListIsEmpty_shouldReturnError() {
        DeleteReq req = new DeleteReq(List.of());

        BasicRes res = quizService.delete(req);

        assertEquals(ReplyMessage.QUIZ_ID_ERROR.getCode(), res.getCode());
    }

    @Test
    void delete_whenQuizIdIsZero_shouldReturnError() {
        DeleteReq req = new DeleteReq(List.of(0));

        BasicRes res = quizService.delete(req);

        assertEquals(ReplyMessage.QUIZ_ID_ERROR.getCode(), res.getCode());
    }

    @Test
    void delete_whenValidIds_shouldReturnSuccess() {
        DeleteReq req = new DeleteReq(List.of(1, 2));

        BasicRes res = quizService.delete(req);

        assertEquals(ReplyMessage.SUCCESS.getCode(), res.getCode());
    }
}
