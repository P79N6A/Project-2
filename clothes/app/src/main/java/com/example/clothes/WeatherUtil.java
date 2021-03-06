package com.example.clothes;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.json.JSONObject;

public class WeatherUtil {

        /*** 現在天氣*/
        public static final String NOW_URL = "https://opendata.cwb.gov.tw/fileapi/v1/opendataapi/O-A0003-001?Authorization=CWB-6BB38BEE-559E-42AB-9AAD-698C12D12E22&downloadType=WEB&format=JSON";
        /*** 省份页面（省）*/
        public static final String PROVINCE_URL = "http://m.weathercn.com/common/province.jsp";
        /*** 城市頁面<br/>* pid=%s中%s表示城市編號 例：http://m.weathercn.com/common/dis.do?pid=010101*/
        public static final String DISTRICT_URL = "https://opendata.cwb.gov.tw/fileapi/v1/opendataapi/O-A0001-001?Authorization=CWB-6BB38BEE-559E-42AB-9AAD-698C12D12E22&downloadType=WEB&format=XML";
        /*** 區<br/>* did=%s中%s表示縣區編號<br/>* pid=%s中%s表示城市編號<br/>
         * 例：http://m.weathercn.com/common/cout.do?did=01010101&pid=010101*/
        public static final String COUNTY_URL = "https://opendata.cwb.gov.tw/fileapi/v1/opendataapi/F-C0032-005?Authorization=CWB-6BB38BEE-559E-42AB-9AAD-698C12D12E22&downloadType=WEB&format=XML";
        /*** 7天天氣預報頁面<br/>
         * cid=%s中%s表示區編號<br/>
         * 例：http://m.weathercn.com/common/7d.do?cid=0101010110*/
        public static final String REPORT7_URL = "http://m.weathercn.com/common/7d.do?cid=%s";
        /*** 生活指數頁面<br/>* cid=%s中%s表示區编號*/
        public static final String REPORT_MORE_URL = "http://m.weathercn.com/common/zslb.do?cid=%s";

        /*** 保存城市編碼的xml文档<br/>
         * 保存了具体的區縣所对应的編碼，例如<br/>
         * <county><br/>
         * <name>北京</name><br/>
         * <code>0101010110</code><br/>
         * </county>*/
        public static final String XML_FILE = "./weathercn.xml";

        private List<Weekweather> weatherReportList = new ArrayList<Weekweather>();

        /*** 啟動的时候，首先檢查weathercn.xml是否存在，如果不存在的話，重新從m.weathercn.com獲取，
         * 只有第一次的时候會獲取。*/
        static {
            try {
                prepareXML();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /*** 返回指定城市，指定日期的天氣預報。<br/>** @param city
         *            城市，如"北京"。* @param cal
         *            日期。* @return 如果城市正確，并且日期在7天以内，那麼返回天氣訊息。否則返回null。*/
        public Weekweather getWeatherReport(String city, Calendar cal) {
            String dateStr = cal.get(Calendar.MONTH) + "月"
                    + cal.get(Calendar.DAY_OF_MONTH) + "日";
            return getWeatherReport(city, dateStr);
        }

        /*** 返回指定城市，指定日期的天氣預報。<br/>
         ** @param city
         *            城市，如"北京"。* @param date
         *            日期，格式為"1月20日"。* @return 如果城市正確，并且日期在7天以内，那么返回天氣訊息。否則返回null。*/
        public Weekweather getWeatherReport(String city, String date) {
            for (Weekweather report : getWeatherReports(city)) {
                if (report.getDate().equals(date)) {
                    return report;
                }
            }

            return null;
        }

        /*** 返回指定城市的天气預報（7天内）*
         * @param city* @return 返回指定城市的天氣預報（7天内），如果指定的城市錯誤，返回空的list，list.size()=0*/
        public List<Weekweather> getWeatherReports(String city) {
            List<Weekweather> list = new ArrayList<Weekweather>();
            try {

                String weatherPage = getWeatherReportPage(city);

                List<String> reportStrList = getAllMathers(weatherPage,
                        "(?<=class=\"b\">)[\\s\\S]+?<br>[\\s\\S]+?(?=</)");
                for (String reportStr : reportStrList) {
                    String weather = reportStr.trim().replaceAll(" ", "")
                            .replaceAll("<br>\r\n\r\n", "\r\n")
                            .replaceAll("<br>", "");

                    String[] str = weather.split("\r\n");
                    if (str.length > 5) {
                        Weekweather report = new Weekweather();

                        int i = 0;
                        String dateStr = str[i++].trim();

                        report.setCity(city);
                        report.setDate(getMatcher(dateStr, ".+(?=\\()"));
                        report.setWeekDay(getMatcher(dateStr, "(?<=\\().+?(?=\\))"));
                        report.setDayOrNight(str[i++].trim());
                        report.setWeather(str[i++].trim());
                        report.setTemperature(str[i++].trim());
                        report.setWindDir(str[i++].trim());
                        report.setWind(str[i++].trim());

                        list.add(report);
                        if (str.length > 10) {
                            report = new Weekweather();
                            report.setCity(city);
                            report.setDate(getMatcher(dateStr, ".+(?=\\()"));
                            report.setWeekDay(getMatcher(dateStr,
                                    "(?<=\\().+?(?=\\))"));
                            report.setDayOrNight(str[i++].trim());
                            report.setWeather(str[i++].trim());
                            report.setTemperature(str[i++].trim());
                            report.setWindDir(str[i++].trim());
                            report.setWind(str[i++].trim());
                            list.add(report);
                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            this.weatherReportList = list;
            return this.weatherReportList;

        }

        /*** 返回字串中第一个符合regex的字串，如果沒有符合的，返回空字串
         ** @param str*            字符串* @param regex*            正则表达式* @return*/
        public static String getMatcher(String str, String regex) {
            Matcher mat = Pattern.compile(regex).matcher(str);
            if (mat.find()) {
                return mat.group();
            } else {
                return "";
            }
        }

        /*** 返回字串str中所有符合regex的子字串。*
         * @param str
         * @param regex
         * @return*/
        public static List<String> getAllMathers(String str, String regex) {
            List<String> strList = new ArrayList<String>();
            Matcher mat = Pattern.compile(regex).matcher(str);
            while (mat.find()) {
                strList.add(mat.group());
            }
            return strList;
        }

        /*** 从m.weathercn.com獲取城市(區域county)和城市所對應的編號(區域編號cid)。<br/>
         * 並保存到xml文件"weathercn.xml"。如果已经存在weathercn.xml文件，那么不會再次獲取。*
         * @throws Exception*/
        private static void prepareXML() throws Exception {
            /*** 如果xml文件已经存在，不用再次獲取。*/
            File file = new File(XML_FILE);
            if (file.exists()) {
                // 提示xml文件位置
                System.out.println("在下面的路径中找到XML文件 " + file.getCanonicalPath());
                return;
            }

            // 用DOM創建XML文檔
            Document doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder().newDocument();
            // 创建XML文檔root element
            Element root = doc.createElement("root");
            doc.appendChild(root);

            // 省province
            //
            WebPageUtil webPageUtil = new WebPageUtil().processUrl(PROVINCE_URL);

            String provincePage = webPageUtil.getWebContent();
            Hashtable<String, String> provinceTable = parseProvincePage(provincePage);
            for (String province : provinceTable.keySet()) {
                // 進度提示
                System.out.println(String.format("正在獲取%s的城市訊息...", province));
                Element eleProvince = doc.createElement(province);
                eleProvince.setAttribute("pid", provinceTable.get(province));
                root.appendChild(eleProvince);

                String districtPage = new WebPageUtil().processUrl(
                        String.format(DISTRICT_URL, provinceTable.get(province)))
                        .getWebContent();

                Hashtable<String, String> districtTable = parseDistrictPage(districtPage);
                for (String district : districtTable.keySet()) {
                    Element eleDistrict = doc.createElement(district);
                    eleDistrict.setAttribute("did", districtTable.get(district));
                    eleProvince.appendChild(eleDistrict);

                    // long time = System.currentTimeMillis();
                    String countyPage = new WebPageUtil().processUrl(
                            String.format(COUNTY_URL, districtTable.get(district),
                                    provinceTable.get(province))).getWebContent();
                    Hashtable<String, String> countyTable = parseCountyPage(countyPage);
                    for (String county : countyTable.keySet()) {
                        Element eleCounty = doc.createElement(county);
                        eleCounty.setAttribute("cid", countyTable.get(county));
                        eleDistrict.appendChild(eleCounty);
                        // System.out.println(String.format("%s->%s->%s %s",
                        // province, district, county,
                        // System.currentTimeMillis()-time));

                    }
                }
                // 进度提示，不需要可以注释掉
                System.out.println(String.format("已成功獲取%s的城市訊息", province));
            }

            // 将获取到的信息保存到xml文件中
            TransformerFactory tFactory = TransformerFactory.newInstance();
            Transformer transformer = tFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(
                    "{http://xml.apache.org/xslt}indent-amount", "2");
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(file);
            transformer.transform(source, result);
            System.out.println("XML文件已經被成功創建 " + file.getCanonicalPath());

        }

        /*** 从m.weathercn.com的市頁面中獲取區縣訊息。<br/>
         * 例如：從成都市頁面中獲取新津，雙流等區縣的編號。*
         * @param page
         * @return*/
        public static Hashtable<String, String> parseCountyPage(String page) {

            return getKeyValues(page,
                    "<a href=\"index.do\\?cid=?.+?&pid=.+?class=\"c\">.+?</a>",
                    "(?<=>).+?(?=</a>)", "(?<=cid=)[0-9]+");
        }

        /*** 从m.weathercn.com的省頁面中獲取省市訊息。<br/>
         * 例如：從四川省的頁面獲取成都，绵羊等市的編號。*
         * @param page
         * @return*/
        public static Hashtable<String, String> parseDistrictPage(String page) {

            return getKeyValues(page, "<a href=\"cout.do\\?.+?class=\"c\">.+?</a>",
                    "(?<=>).+?(?=</a>)", "(?<=did=)[0-9]+");
        }

        /*** 从m.weathercn.com的國內頁面中獲取省市訊息。<br/>
         * 例如：從國內頁面獲取四川省，山東省，北京市等的編號。
         ** @param page* @return*/
        public static Hashtable<String, String> parseProvincePage(String page) {

            return getKeyValues(page, "<a href=\"dis.do?.+?class=\"c\">.+?</a>",
                    "(?<=>).+?(?=</a>)", "(?<=pid=)[0-9]+");
        }

        /*** 从頁面里面獲取所需要的訊息。
         *
         * @param webPage
         *            網頁
         * @param tagRegex
         *            正则表达式，用以獲取包含key和value的内容，保存在字串tag中
         * @param keyRegex
         *            正则表达式，用以从tag獲取key的值
         * @param valueRegex
         *            正则表达式，用以从tag獲取value的值
         * @return 返回網頁中所有匹配的key和value，如果没有，返回一个空的table，table.size()=0*/
        public static Hashtable<String, String> getKeyValues(String webPage,
                                                             String tagRegex, String keyRegex, String valueRegex) {
            Hashtable<String, String> table = new Hashtable<String, String>();

            for (String tag : getAllMathers(webPage, tagRegex)) {
                table.put(getMatcher(tag, keyRegex), getMatcher(tag, valueRegex));
            }

            return table;
        }

        /*** 獲取指定城市或者區域的天氣預報頁面。
         *
         * @param city
         *            城市
         * @return 返回天氣预报頁面的源代碼。錯誤或者城市不正確等则返回空字串。
         * @throws Exception*/
        public String getWeatherReportPage(String city) throws Exception {
            Document doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder().parse(XML_FILE);
            NodeList nodeList = doc.getElementsByTagName(city);
            for (int i = 0; i < nodeList.getLength(); i++) {
                Element ele = (Element) nodeList.item(i);
                if (!ele.getAttribute("cid").equals("")) {
                    return new WebPageUtil().processUrl(
                            String.format(REPORT7_URL, ele.getAttribute("cid")))
                            .getWebContent();
                }
            }
            return "";
        }

}
