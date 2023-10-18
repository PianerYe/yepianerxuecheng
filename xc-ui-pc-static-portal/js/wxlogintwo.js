!function (a, b) {
    function d(a) {
        var d = b.createElement("iframe");
        d.name = "tempUserId";
        e = "https://yd.jylt.cc/api/wxLogin/tempUserId?secret=" + a.secret + "&width=" + a.width;
        fetch(e)
            .then(response => response.json())
            .then(data => {
                // 访问JSON数据的属性
                item = data.data.qrUrl;
                tempUserId = data.data.tempUserId;
                console.log(item)
                console.log(a.id)
                d.src = item,
                    d.frameBorder = "0",
                    d.allowTransparency = "true",
                    d.scrolling = "no",
                    d.width = "430px",
                    d.height = "430px";
                d.name = tempUserId;
                var f = b.getElementById(a.id);
                f.innerHTML = "", f.appendChild(d)
                console.log(tempUserId)
            })
            .catch(error => {
                console.error('API请求失败', error);
            });

    }
    a.WxLoginTwo = d
    console.log(d)
}(window, document);

