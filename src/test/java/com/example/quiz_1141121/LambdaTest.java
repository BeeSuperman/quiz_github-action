package com.example.quiz_1141121;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class LambdaTest {
@Test
public void test() {
	List<String> list=new ArrayList<>(List.of("A","B","C"));
	//foreach
	for(String item:list) {
		System.out.println(item);
		
	}
	//現代化Java：箭頭函數：collection自定義的foreach
	list.forEach((item)->{
		System.out.println(item);
		
	});
	//1.參數個數只有一個時候，小括號可以省略。2.沒有參數的時候必須有小括號
	list.forEach(item->{
		System.out.println(item);
		
	});
	/*變形：當{}裡面只有一行的時候，{}可以省略，但同時也要把{}裡面的程式碼的結尾的;去掉、不能有。
	 * 普通的是只有省略{}，沒有去掉；也可以
	 */
	list.forEach(item->
		System.out.println(item)
		
	);
	//講解：
	/*
	 * Map.of(1,"A", 2,"B", 3,"C")
这是 Java 9 引入的静态工厂方法

创建一个不可变的 Map，包含 3 个键值对：

键 1 → 值 "A"

键 2 → 值 "B"

键 3 → 值 "C"

返回的类型是 Map<Integer, String>
	 */
	/*
	 * new HashMap<>(...)
这是 HashMap 的拷贝构造函数

接收一个现有的 Map 作为参数

创建一个新的、可变的 HashMap，并将传入 Map 中的所有条目复制进来.
最终 map 是一个可变的 HashMap，初始包含三个条目：
	 */
	/*
	 * 注意事项
Map.of() 最多支持 10 个键值对，超过需要用 Map.ofEntries()

Map.of() 创建的 Map 不允许 null 键或 null 值
	 */
	//參數個數有2個或以上的時候，必須有（）
	Map<Integer,String> map=new HashMap<>(Map.of(1,"A",2,"B",3,"C"));
	map.forEach((k,v)->{//k:key,v:value
		System.out.printf("%d : %s \n",k,v);
		
	});
	
}
}
