$(document).ready(function() {
    // Initialize rep inputs for the auto-expanded day
    $('.collapse.show').find('.sets-input').each(function() {
        if (typeof generateSetInputs === 'function') {
            generateSetInputs(this);
        }
    });

    // --- Progress Chart Logic ---
    let progressChart = null;

    $('#progressExerciseSelect').change(function() {
        const exerciseId = $(this).val();
        if (!exerciseId) return;

        console.log("Fetching progress for exercise:", exerciseId);

        const token = $("meta[name='_csrf']").attr("content");
        const header = $("meta[name='_csrf_header']").attr("content");

        fetch(`/workouts/api/progress?exerciseId=${exerciseId}`, {
            headers: {
                'Accept': 'application/json',
                [header]: token
            }
        })
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok: ' + response.statusText);
            }
            return response.json();
        })
        .then(data => {
            console.log("Progress data received:", data);

            const chartMessage = document.getElementById('chartMessage');
            const canvas = document.getElementById('progressChart');

            if (!data || data.length === 0) {
                console.log("No data points found.");
                if (progressChart) {
                    progressChart.destroy();
                    progressChart = null;
                }
                chartMessage.style.display = 'block';
                return;
            }

            chartMessage.style.display = 'none';
            const labels = data.map(d => d.date);
            const weights = data.map(d => d.weight);

            const ctx = canvas.getContext('2d');

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
                    labels: labels,
                    datasets: [{
                        label: 'Max Weight (kg)',
                        data: weights,
                        borderColor: 'rgba(75, 192, 192, 1)',
                        backgroundColor: 'rgba(75, 192, 192, 0.2)',
                        borderWidth: 2,
                        tension: 0.1,
                        pointRadius: 5,
                        pointHoverRadius: 7
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    scales: {
                        y: {
                            beginAtZero: false,
                            title: {
                                display: true,
                                text: 'Weight (kg)'
                            }
                        },
                        x: {
                            title: {
                                display: true,
                                text: 'Date'
                            }
                        }
                    },
                    plugins: {
                        legend: {
                            display: true,
                            position: 'top'
                        },
                        tooltip: {
                            enabled: true
                        }
                    }
                }
            });
        })
        .catch(error => console.error('Error fetching progress data:', error));
    });
});
