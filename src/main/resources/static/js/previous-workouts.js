document.addEventListener('DOMContentLoaded', function() {
    const dateInput = document.getElementById('workoutDateSelect');
    const workoutDetailsEl = document.getElementById('workout-details');
    const token = $("meta[name='_csrf']").attr("content");
    const header = $("meta[name='_csrf_header']").attr("content");

    dateInput.addEventListener('change', function() {
        const dateStr = this.value;
        if (!dateStr) {
            workoutDetailsEl.innerHTML = '<p class="text-muted">No date selected.</p>';
            return;
        }

        workoutDetailsEl.innerHTML = '<p>Loading...</p>';

        fetch(`/workouts/api/by-date?date=${dateStr}`, {
            headers: {
                'Accept': 'application/json',
                [header]: token
            }
        })
        .then(response => response.json())
        .then(data => {
            if (data && data.length > 0) {
                let html = `<h5>Workout on ${dateStr}</h5>`;
                html += '<ul class="list-group">';
                data.forEach(ex => {
                    let details = '';
                    if (ex.exerciseName !== 'RESTDAY' && ex.exerciseName !== 'Restday') {
                        details = `<br><small class="text-muted">
                                       Reps: ${ex.reps} | Weight: ${ex.weight} kg
                                   </small>`;
                    }
                    html += `<li class="list-group-item">
                                <strong>${ex.exerciseName}</strong> - ${ex.sets} Sets
                                ${details}
                             </li>`;
                });
                html += '</ul>';
                workoutDetailsEl.innerHTML = html;
            } else {
                workoutDetailsEl.innerHTML = '<div class="alert alert-info">No workout logged for this day.</div>';
            }
        })
        .catch(error => {
            console.error('Error fetching workout data:', error);
            workoutDetailsEl.innerHTML = '<p class="text-danger">Could not load workout data.</p>';
        });
    });
});