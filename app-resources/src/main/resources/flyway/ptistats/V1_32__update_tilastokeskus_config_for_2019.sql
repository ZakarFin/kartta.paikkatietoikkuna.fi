UPDATE oskari_statistical_datasource SET config='{
	"url": "https://pxnet2.stat.fi/pxweb/api/v1/{language}/Kuntien_avainluvut/2019/kuntien_avainluvut_2019_aikasarja.px",
	"info": {
		"url": "http://www.tilastokeskus.fi"
	},
	"regionKey": "Alue 2019",
	"indicatorKey": "Tiedot",
  "hints" : {
    "dimensions" : [ {
      "id" : "Vuosi",
      "sort" : "DESC"
    }]
  }
}' where locale LIKE '%Tilastokeskus%';