// This function needs to be global for the inline 'onchange' attribute.
window.addExerciseSelectIfNeeded = function(currentSelect) {
    if (!currentSelect.value) return;

    const container = currentSelect.closest('.exercise-select-container');
    if (!container) return;

    const allWrappers = container.querySelectorAll(".exercise-select");
    const currentWrapper = currentSelect.closest('.exercise-select');
    
    if (currentWrapper !== allWrappers[allWrappers.length - 1]) return;

    const newWrapper = currentWrapper.cloneNode(true);
    newWrapper.classList.add("mt-2");
    
    const newSelect = newWrapper.querySelector('select');
    newSelect.value = "";
    newSelect.removeAttribute("required");
    
    const newInput = newWrapper.querySelector('input');
    newInput.value = "3"; 
    
    container.appendChild(newWrapper);
};

$(document).ready(function() {
    // --- Logic for batch saving exercises in the modal ---
    $('#save-exercises-btn').click(function() {
        const updates = [];
        // Find containers only within the editSplitModal to avoid conflicts
        $('#editSplitModal .day-column').each(function() {
            const container = $(this).find('.exercise-select-container');
            const dayId = container.data('day-id');
            if (!dayId) return;

            const exercisesToAdd = [];
            const exerciseIdsToDelete = [];

            // Collect exercises to add
            $(this).find('.exercise-select').each(function() {
                const select = $(this).find('select');
                const input = $(this).find('input');
                const exerciseValue = select.val(); // This is now a string like "S-1" or "C-5"
                let sets = input.val();
                if (!sets || isNaN(sets) || sets < 1) sets = 3;

                if (exerciseValue) {
                    exercisesToAdd.push({ exerciseValue: exerciseValue, sets: parseInt(sets) });
                }
            });

            // Collect exercises to delete
            $(this).find('.exercise-item.deleted-exercise').each(function() {
                const exId = $(this).data('exercise-id');
                if (exId) {
                    exerciseIdsToDelete.push(parseInt(exId));
                }
            });

            if (exercisesToAdd.length > 0 || exerciseIdsToDelete.length > 0) {
                updates.push({
                    dayId: dayId,
                    exercises: exercisesToAdd,
                    exerciseIdsToDelete: exerciseIdsToDelete
                });
            }
        });

        if (updates.length > 0) {
            const token = $("meta[name='_csrf']").attr("content");
            const header = $("meta[name='_csrf_header']").attr("content");

            fetch('/training/profile/me/batch-update-exercises', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', [header]: token },
                body: JSON.stringify({ updates: updates }),
            })
            .then(response => response.ok ? response.json() : Promise.reject('Network response was not ok'))
            .then(() => window.location.reload())
            .catch(error => {
                console.error('Error:', error);
                alert("Fehler beim Speichern! Bitte versuchen Sie es erneut.");
            });
        } else {
            window.location.reload();
        }
    });

    $(document).on('click', '.set-active-split-btn', function(e) {
        e.preventDefault();
        e.stopPropagation();
        const splitId = $(this).data('split-id');
        if (confirm('Möchten Sie diesen Split als aktiv setzen?')) {
            const form = $('<form action="/training/profile/me/set-active-split" method="post" style="display:none;">' +
                '<input type="hidden" name="splitId" value="' + splitId + '" />' +
                '<input type="hidden" name="_csrf" value="' + $("meta[name='_csrf']").attr("content") + '" />' +
                '</form>');
            $('body').append(form);
            form.submit();
        }
    });

    $(document).on('click', '.delete-split-btn', function(e) {
        e.preventDefault();
        e.stopPropagation();
        const splitId = $(this).data('split-id');
        if (confirm('Möchten Sie diesen Split wirklich löschen?')) {
            const form = $('<form action="/training/profile/me/delete-split" method="post" style="display:none;">' +
                '<input type="hidden" name="splitId" value="' + splitId + '" />' +
                '<input type="hidden" name="_csrf" value="' + $("meta[name='_csrf']").attr("content") + '" />' +
                '</form>');
            $('body').append(form);
            form.submit();
        }
    });

    // --- Logic for Marking/Undoing Exercise Deletion in the modal ---
    $(document).on('click', '#editSplitModal .mark-delete-btn', function(e) {
        e.preventDefault();
        const item = $(this).closest('.exercise-item');
        item.addClass('deleted-exercise');
        $(this).hide();
        item.find('.undo-delete-btn').show();
    });

    $(document).on('click', '#editSplitModal .undo-delete-btn', function(e) {
        e.preventDefault();
        const item = $(this).closest('.exercise-item');
        item.removeClass('deleted-exercise');
        $(this).hide();
        item.find('.mark-delete-btn').show();
    });
});
