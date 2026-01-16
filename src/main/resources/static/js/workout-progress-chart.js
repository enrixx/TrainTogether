$(document).ready(function() {
    // Initialize rep inputs for the auto-expanded day
    $('.collapse.show').find('.sets-input').each(function() {
        if (typeof generateSetInputs === 'function') {
            generateSetInputs(this);
        }
    });

    // --- Progress Chart Logic ---
    let progressChart = null;
    let chartData = {
        blue: [],
        red: [],
        green: []
    };
    let currentTimeRange = 'all';

    // Colors configuration
    const colors = {
        blue: { border: 'rgba(54, 162, 235, 1)', bg: 'rgba(54, 162, 235, 0.2)' },
        red: { border: 'rgba(255, 99, 132, 1)', bg: 'rgba(255, 99, 132, 0.2)' },
        green: { border: 'rgba(75, 192, 192, 1)', bg: 'rgba(75, 192, 192, 0.2)' }
    };

    // Handle Exercise Selection
    $('.progress-exercise-select').change(function() {
        const exerciseId = $(this).val();
        const color = $(this).data('color');
        const selectElement = this;
        const exerciseName = selectElement.options[selectElement.selectedIndex].text;

        if (!exerciseId) {
            chartData[color] = [];
            updateChart();
            return;
        }

        console.log(`Fetching progress for ${color} exercise:`, exerciseId);

        const token = $("meta[name='_csrf']").attr("content");
        const header = $("meta[name='_csrf_header']").attr("content");

        fetch(`/workouts/api/progress?exerciseId=${exerciseId}`, {
            headers: {
                'Accept': 'application/json',
                [header]: token
            }
        })
        .then(response => {
            if (!response.ok) throw new Error('Network response was not ok');
            return response.json();
        })
        .then(data => {
            console.log(`Data received for ${color}:`, data);
            chartData[color] = data.map(d => ({
                x: d.date, 
                y: d.weight,
                dateObj: new Date(d.date)
            }));
            chartData[color].label = exerciseName;
            updateChart();
        })
        .catch(error => console.error('Error fetching progress data:', error));
    });

    // Handle Time Range Selection
    $('.time-filter-btn').click(function() {
        $('.time-filter-btn').removeClass('active');
        $(this).addClass('active');
        currentTimeRange = $(this).data('range');
        updateChart();
    });

    function getWeekNumber(d) {
        d = new Date(Date.UTC(d.getFullYear(), d.getMonth(), d.getDate()));
        d.setUTCDate(d.getUTCDate() + 4 - (d.getUTCDay()||7));
        var yearStart = new Date(Date.UTC(d.getUTCFullYear(),0,1));
        var weekNo = Math.ceil(( ( (d - yearStart) / 86400000) + 1)/7);
        return d.getUTCFullYear() + "-W" + weekNo;
    }

    function aggregateData(data, range) {
        if (range === 'week' || range === 'month') {
            return data; // No aggregation for short periods
        }

        // Group by Week for 'year' and 'all'
        const groups = {};
        data.forEach(point => {
            const weekKey = getWeekNumber(point.dateObj);
            if (!groups[weekKey]) {
                groups[weekKey] = [];
            }
            groups[weekKey].push(point);
        });

        const aggregated = [];
        for (const key in groups) {
            const points = groups[key];
            // Find max weight in this week
            const maxPoint = points.reduce((prev, current) => (prev.y > current.y) ? prev : current);
            aggregated.push(maxPoint);
        }

        // Sort again by date
        return aggregated.sort((a, b) => a.dateObj - b.dateObj);
    }

    function updateChart() {
        const ctx = document.getElementById('progressChart').getContext('2d');
        const chartMessage = document.getElementById('chartMessage');

        // Filter data based on time range
        const now = new Date();
        let minDate = null;

        if (currentTimeRange === 'week') {
            minDate = new Date();
            minDate.setDate(now.getDate() - 7);
        } else if (currentTimeRange === 'month') {
            minDate = new Date();
            minDate.setMonth(now.getMonth() - 1);
        } else if (currentTimeRange === 'year') {
            minDate = new Date();
            minDate.setFullYear(now.getFullYear() - 1);
        }

        const datasets = [];
        let hasData = false;
        let allLabels = new Set();

        ['blue', 'red', 'green'].forEach(color => {
            if (chartData[color] && chartData[color].length > 0) {
                let filteredData = chartData[color];
                
                if (minDate) {
                    filteredData = filteredData.filter(d => d.dateObj >= minDate);
                }

                // Aggregate data if needed
                filteredData = aggregateData(filteredData, currentTimeRange);

                if (filteredData.length > 0) {
                    hasData = true;
                    filteredData.forEach(d => allLabels.add(d.x));

                    datasets.push({
                        label: chartData[color].label || 'Exercise',
                        data: filteredData.map(d => ({x: d.x, y: d.y})),
                        borderColor: colors[color].border,
                        backgroundColor: colors[color].bg,
                        borderWidth: 2,
                        tension: 0.1,
                        pointRadius: 4,
                        pointHoverRadius: 6
                    });
                }
            }
        });

        if (!hasData) {
            if (progressChart) {
                progressChart.destroy();
                progressChart = null;
            }
            chartMessage.style.display = 'block';
            return;
        }

        chartMessage.style.display = 'none';

        const sortedLabels = Array.from(allLabels).sort();

        if (progressChart) {
            progressChart.destroy();
        }

        if (typeof Chart === 'undefined') {
            console.error("Chart.js library is not loaded!");
            return;
        }

        progressChart = new Chart(ctx, {
            type: 'line',
            data: {
                datasets: datasets
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    y: {
                        beginAtZero: false,
                        title: { display: true, text: 'Weight (kg)' }
                    },
                    x: {
                        type: 'category',
                        labels: sortedLabels,
                        title: { display: true, text: 'Date' }
                    }
                },
                plugins: {
                    legend: { display: true, position: 'top' },
                    tooltip: { enabled: true }
                }
            }
        });
    }
});
