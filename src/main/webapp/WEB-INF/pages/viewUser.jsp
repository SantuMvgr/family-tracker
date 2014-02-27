<%@ include file="/common/taglibs.jsp" %>

<table class="weather" id="weatherTable" border="1">
<thead></thead>
<tbody></tbody>
</table>

<h1>Top News</h1> <div id="topNewsId"></div>
<h1>Business</h1> <div id="businessNewsId"></div>
<h1>Sports</h1> <div id="sportsNewsId"></div>

<s:hidden name="weatherInfo" id="weatherInfo" />
<s:hidden name="topNewsFeed" id="topNewsFeed" />
<s:hidden name="businessNewsFeed" id="businessNewsFeed" />
<s:hidden name="sportsNewsFeed" id="sportsNewsFeed" />

<script type="text/javascript">
	var strJSON = $('#weatherInfo').val();
	var objJSON = JSON.parse(strJSON);
	$('table#weatherTable TBODY').append('<tr><th colspan="2">' + objJSON.name + '</th></tr>' +
					'<tr><td>' + "Forecast" + '</td><td>' + objJSON.weather[0].main + '</td><tr>' +
					'<tr><td>' + "Current Temp" + '</td><td>' + objJSON.main.temp + ' °C</td><tr>' +
					'<tr><td>' + "Minimum Temp" + '</td><td>' + objJSON.main.temp_min + ' °C</td><tr>' +
					'<tr><td>' + "Maximum Temp" + '</td><td>' + objJSON.main.temp_max + ' °C</td></tr>'
					);
	
	var tmpJSON = $('#topNewsFeed').val();
	var newsJSON = JSON.parse(tmpJSON);
	$.each(newsJSON.articles, function(key, value) {
		$('div#topNewsId').append('<ul><li><a href=' + value.url + '>' + value.title + '</a></ul>');
	});
	
	tmpJSON = $('#businessNewsFeed').val();
	newsJSON = JSON.parse(tmpJSON);
	$.each(newsJSON.articles, function(key, value) {
		$('div#businessNewsId').append('<ul><li><a href=' + value.url + '>' + value.title + '</a></ul>');
	});
	
	tmpJSON = $('#sportsNewsFeed').val();
	newsJSON = JSON.parse(tmpJSON);
	$.each(newsJSON.articles, function(key, value) {
		$('div#sportsNewsId').append('<ul><li><a href=' + value.url + '>' + value.title + '</a></ul>');
	});
</script>