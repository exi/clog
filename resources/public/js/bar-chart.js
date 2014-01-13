$(function() {
    $('div .render-step-chart').each(function() {
        var eldata = $(this).data().stepChart;
        console.log(this.getBoundingClientRect());
        var h = $(this).outerHeight();
        var w = $(this).outerWidth();
        var el = this;
        var margin = {left: 80, right: 50, bottom: 30, top: 30},
            width = w - margin.left - margin.right,
            height = h - margin.top - margin.bottom;

        var url = eldata['base-url'] + eldata.start + '/' + eldata.end + '/' + width;

        $.ajax({url: url,
                dataType: 'json',
                success: function (data) {
                    renderData(data);
                }});

        function renderData(data) {
            var items = data.items;
            if (items.length === 0) {
                return;
            }

            items.forEach(function(d) {
                d.date = new Date(d.date);
            });

            var range = data.range;

            items.sort(function(a, b) { return a.date - b.date; });


            var x = d3.time.scale().range([0, width]);
            var y = d3.scale.linear().range([height, 0]);

            var xAxis = d3.svg.axis().scale(x).orient('bottom');
            var yAxis = d3.svg.axis().scale(y).orient('left');

            x.domain([new Date(data.start), new Date(data.end)]);
            y.domain(d3.extent(items, function(d) { return d.value; }));

            var lineFunction = d3.svg.line()
                .x(function(d) { return x(d.date); })
                .y(function(d) { return y(d.value); });

            var bisectDate = d3.bisector(function(d) { return d.date; }).left;

            var svg = d3.select(el)
                .append('svg')
                .attr('width', w)
                .attr('height', h)
                .append('g')
                    .attr('transform', 'translate(' + margin.left + ',' + margin.top + ')');

            svg.append('g')
               .attr('class', 'x axis')
               .attr('transform', 'translate(0,' + height + ')')
               .call(xAxis);

            svg.append('g')
                .attr('class', 'y axis')
                .call(yAxis)
               .append('text')
                .attr('transform', 'rotate(-90)')
                .attr('y', 6)
                .attr('dy', '.71em')
                .style('text-anchor', 'end')
                .text(eldata['y-axis-label']);

            svg.append('path')
               .datum(items)
               .attr('class', 'line')
               .attr('d', lineFunction);

            var focus = svg.append('g')
                           .attr('class', 'focus')
                           .attr('transform', 'translate( 30, 0)')
                           .style('display', 'none');

            var circle = svg.append('circle')
                            .attr('r', 4.5);

            focus.append('text')
                 .attr('x', 9)
                 .attr('dy', '.35em');

            svg.append('rect')
               .attr('class', 'overlay')
               .attr('width', width)
               .attr('height', height)
               .on('mouseover', function() { focus.style('display', null); })
               .on('mouseout', function() { focus.style('display', 'none'); })
               .on('mousemove', mousemove);

            function mousemove() {
                var x0 = x.invert(d3.mouse(this)[0]),
                    i = Math.min(items.length - 1, bisectDate(items, x0, 1)),
                    d0 = (i > 0 ? items[i - 1] : items[i]),
                    d1 = items[i],
                    d = x0 - d0.date > d1.date - x0 ? d1 : d0;
                circle.attr('transform', 'translate(' + x(d.date) + ', ' + y(d.value) + ')');
                focus.select('text').text((eldata['y-axis-label'] + ': ' + Math.floor(d.value * 100) / 100) + ' On: ' + d.date);
            }
        }
    });
});
