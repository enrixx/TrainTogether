document.addEventListener('DOMContentLoaded', function () {
    let exerciseCounter = 0;

    const addExerciseBtn = document.getElementById('add-exercise-btn');
    if (addExerciseBtn) {
        addExerciseBtn.addEventListener('click', function () {
            const exerciseEntry = document.createElement('div');
            exerciseEntry.classList.add('row', 'mb-2', 'align-items-end');
            
            let optionsHtml = '<option value="">Choose exercise...</option>';
            if (window.userExercises) {
                window.userExercises.forEach(ex => {
                    optionsHtml += `<option value="${ex.id}">${ex.name}</option>`;
                });
            }

            exerciseEntry.innerHTML = `
                <div class="col-md-6">
                    <label class="form-label">Exercise</label>
                    <select name="exercises[${exerciseCounter}].personalExerciseId" class="form-select" required>
                        ${optionsHtml}
                    </select>
                </div>
                <div class="col-md-2">
                    <label class="form-label">Sets</label>
                    <input type="number" name="exercises[${exerciseCounter}].sets" class="form-control" required>
                </div>
                <div class="col-md-2">
                    <label class="form-label">Reps</label>
                    <input type="number" name="exercises[${exerciseCounter}].reps" class="form-control" required>
                </div>
                <div class="col-md-2">
                    <button type="button" class="btn btn-danger remove-exercise-btn">Remove</button>
                </div>
            `;
            document.getElementById('exercise-entries').appendChild(exerciseEntry);
            exerciseCounter++;
        });
    }

    const exerciseEntries = document.getElementById('exercise-entries');
    if (exerciseEntries) {
        exerciseEntries.addEventListener('click', function (e) {
            if (e.target && e.target.classList.contains('remove-exercise-btn')) {
                e.target.closest('.row').remove();
            }
        });
    }

    // Chart.js logic
    const ctx = document.getElementById('progressChart');
    if (ctx) {
        let progressChart = new Chart(ctx.getContext('2d'), {
            type: 'line',
            data: {
                labels: [], // Dates will go here
                datasets: [{
                    label: 'Reps Over Time',
                    data: [], // Reps will go here
                    borderColor: 'rgba(75, 192, 192, 1)',
                    borderWidth: 1
                }]
            }
        });

        const exerciseSelectGraph = document.getElementById('exercise-select-graph');
        if (exerciseSelectGraph) {
            exerciseSelectGraph.addEventListener('change', function() {
                const exerciseId = this.value;
                fetch(`/workouts/progress?exerciseId=${exerciseId}`)
                    .then(response => response.json())
                    .then(data => {
                        progressChart.data.labels = data.labels;
                        progressChart.data.datasets[0].data = data.reps;
                        progressChart.update();
                    });
            });
        }
    }
});
