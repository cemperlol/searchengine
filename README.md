# SearchEngine 
SearchEngine – проект поискового движка, в котором реализованы как индексация отдельных веб-страниц и веб-сайтов целиком, так и поиск по проиндексированным веб-страницам

---

## Примененные технологии
- Spring Core
- Spring Beans
- Spring Boot
- Spring Web
- Spring JPA 
- Hibernate
- Thymeleaf
- Fork/Join Framework
- JSoup
- MySQL
- Liquibase
- Lucene Framework
- Stream API
- Библиотека Apache Commons
- Lombok
- Maven
 
 ---
 
## Особенности проекта
В ходе работы над проектом пришлось столкнуть со множеством проблем:
- Различные ошибки при подключении к веб-странице с помощью JSoup 
- Большое количества запросов к базе данных
- Перегрузка кучи при удалении большого количества записей из таблиц с помощью метода deleteAll()
`java.lang.OutOfMemoryError: Java heap space`
- Определение является ли ссылка частью данного веб-сайта или другого
- Остановка всех задач во всех потоках и корректная запись результата
- Определение возможных ошибок в ходе индексации и их корректная обработка

Из-за всех этих проблем была выработано решение, позволяющее проводить параллельные вычисления над **каждой** задачей одновременно, не исчерпая ресурсы ЭВМ, корректно обрабатывать все возникающие ошибки в ходе индексации,  и быстро выводить результат поиска без лишних запросов к базе данных
 
 ---
 
## Какие задачи решает данный проект
SearchEngine удовлетворяет потребность человека в сборе и поиске информации
 
 ---
 
## Демо
- Статистика:

![showcase_statistics](https://github.com/cemperlol/searchengine/assets/98911150/02ead659-1496-45a4-bb75-1708715e5e01)

- Индексация:

![showcase_indexing](https://github.com/cemperlol/searchengine/assets/98911150/b445afbe-105a-4354-99ba-742f63c6e0aa)

- Остановка индексации:

![showcase_stop_indexing](https://github.com/cemperlol/searchengine/assets/98911150/23216f72-d058-4273-b9de-440009ad3724)

- Поиск по проиндексированным сайтам

![showcase_search](https://github.com/cemperlol/searchengine/assets/98911150/f4a244ab-e59b-49cf-a4cb-f39a4e100f29)
 
 ---
 
## Инструкция по запуску:
**1.** Скомпилируйте код с помощью вкладки `Maven`, выбрав опции, как на снимке экран, а затем нажав на `Run Maven Build` (зеленый треугольник):

![packaging_app](https://github.com/cemperlol/searchengine/assets/98911150/3b11f6c1-ef4c-438b-a8d8-f32db44d05e3)
<br>

**2.** Дождитесь окончания упаковки проекта
**3.** Создайте директорию для готово проекта в удобном для Вас месте, но за пределами самого проекта. Пример:

![creatk](https://github.com/cemperlol/searchengine/assets/98911150/eefc0c25-5da6-4131-a391-d1fc1488f53c)
<br>

**4.** Перейдите в папку `target` и скопируйте файл `SearchEngine.jar` в созданную Ваши директорию

![copying_jar](https://github.com/cemperlol/searchengine/assets/98911150/0c604a29-baf5-4642-9ce1-29b6f85c2963)
<br>

**5.** Скопируйте папку `config` в Вашу директорию:

![copying_config](https://github.com/cemperlol/searchengine/assets/98911150/621ee26e-8d5f-4837-a034-37b7805347c6)
<br>

**6.** Настройте список сайтов для индексации в файле application.yaml, который находится в скопированной Вами папке config (файл можно открыть с помощью любого текстового редактора. Пример настройки списка сайтов для индексации
<figure>
  <img src="https://github.com/cemperlol/searchengine/assets/98911150/bff80702-3f78-43f4-9e08-249663e2ccb1">
  <br>
  <figcaption>
    <i>
      <p>
        &nbsp&nbsp&nbsp&nbsp&nbsp<strong>Важно:</strong> если Вы хотите добавить свои файлы для индексации, то Вам необходимо добавлять их <strong>строго</strong> в
        том же формате, что и на снимке экрана выше
      </p>
      <p>
        &nbsp&nbsp&nbsp&nbsp&nbsp"- url" – ссылка на главную страницу сайта в формате: <code>http://www.example.com</code> – где <code>http</code> может быть
        заменено на <code>https</code>, а <code>www.</code> может отсутствовать<br>
      </p>
      <p>
        &nbsp&nbsp&nbsp&nbsp&nbsp"name" – название сайта        
      </p>
    </i>
  </figcaption>
</figure>

**7.** Откройте командую строку (Windows) или терминал (macOs и перейдите в директорию, куда Вы скопировали файл `SearchEngine.jar`

**8.** Запустите приложение с помощью команды: `java -jar SearchEngine.jar`
![starting_app](https://github.com/cemperlol/searchengine/assets/98911150/ca1bc2a4-810c-4edd-9a35-5243c3160e7b)

**9.** Чтобы начать пользоваться приложением: откройте браузер и перейдите по url [localhost:8080](http://localhost:8080)

![showcase_connection](https://github.com/cemperlol/searchengine/assets/98911150/749968c4-a358-44e0-bd03-00486f786079)

**10.** Для закрытия приложения перейдите в командую строку или терминал – в зависимости от Вашей операционной системы, и нажмите комбинацию клавиш `Ctrl + C`, чтобы закрыть программу

![shutting_down_app](https://github.com/cemperlol/searchengine/assets/98911150/c162bf1a-d913-4484-bb6b-912fd816d0e5)
<br>

**Важно:** для использования приложения необходим установленный локально MySQL


