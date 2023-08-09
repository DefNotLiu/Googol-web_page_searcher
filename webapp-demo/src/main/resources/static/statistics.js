// statistics.js

// Fetch the statistics data from the server
fetch('/getStatistics')
    .then(response => response.json())
    .then(data => {
        // Access the top 10 results from the data
        const top10Results = data.slice(0, 10);

        // Get the <ul> element
        const top10ResultsList = document.getElementById('top-10-results');

        // Iterate over the top 10 results and create <li> elements
        top10Results.forEach(result => {
            const li = document.createElement('li');
            li.textContent = result;
            top10ResultsList.appendChild(li);
        });
    })
    .catch(error => console.error('Error:', error));
