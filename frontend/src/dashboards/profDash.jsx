import React, { useEffect, useState } from 'react';
import Header from '../components/header';

const ProfDash = () => {
  const [bookings, setBookings] = useState([]);
  const [comments, setComments] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const token = localStorage.getItem('jwt');
  const userEmail = localStorage.getItem('email');

  useEffect(() => {
    const fetchData = async () => {
      setLoading(true);
      try {
        // 1. Get professor profile to determine isCultural
        const profRes = await fetch(`http://localhost:8080/api/professors/${userEmail}`, {
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
          }
        });
        if (!profRes.ok) throw new Error('Failed to fetch professor profile');
        const professor = await profRes.json();

        // 2. Get all bookings
        const bookingsRes = await fetch('http://localhost:8080/api/bookings', {
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
          }
        });
        if (!bookingsRes.ok) throw new Error('Failed to load bookings');
        const allBookings = await bookingsRes.json();

        let relevantBookings = [];

        if (professor.isCultural) {
          // Cultural professor: see all bookings
          relevantBookings = allBookings;
        } else {
          // Non-cultural: fetch clubs advised by this professor
          const clubsRes = await fetch('http://localhost:8080/api/clubs', {
            headers: {
              'Authorization': `Bearer ${token}`,
              'Content-Type': 'application/json'
            }
          });
          if (!clubsRes.ok) throw new Error('Failed to fetch clubs');
          const clubs = await clubsRes.json();
          const advisedClubs = clubs
            .filter(club => club.facultyHeadEmail === userEmail)
            .map(club => club.name);

          // Only bookings for clubs they advise
          relevantBookings = allBookings.filter(
            booking => advisedClubs.includes(booking.clubName)
          );
        }

        // 3. Filter out bookings already approved/rejected by this professor AND globally rejected bookings
        const filteredBookings = relevantBookings.filter(
          booking =>
            !booking.approvals?.some(approval => approval.approverEmail === userEmail) &&
            booking.overallStatus !== 'REJECTED' // New rejection filter
        );

        setBookings(filteredBookings);
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, [token, userEmail]);

  // ... rest of the component remains unchanged ...
  const handleCommentChange = (bookingKey, value) => {
    setComments(prev => ({ ...prev, [bookingKey]: value }));
  };

  const handleApproval = async (booking, status) => {
    const bookingKey = `${booking.block}-${booking.roomNo}-${booking.startTime}`;
    const comment = comments[bookingKey] || '';
    try {
      const response = await fetch(
        `http://localhost:8080/api/bookings/${booking.block}/${booking.roomNo}/${booking.startTime}/approvals`,
        {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            approverEmail: userEmail,
            status: status,
            comments: comment
          })
        }
      );
      if (!response.ok) throw new Error('Approval action failed');
      setBookings(bookings.filter(b =>
        !(b.block === booking.block && b.roomNo === booking.roomNo && b.startTime === booking.startTime)
      ));
      setComments(prev => {
        const newComments = { ...prev };
        delete newComments[bookingKey];
        return newComments;
      });
    } catch (err) {
      setError(err.message);
    }
  };

  if (loading) return <div>Loading...</div>;
  if (error) return <div>{error}</div>;

  return (
    <>
      <Header />
      <div className="prof-dash-container">
        <h2>Pending Approvals</h2>
        <table className="prof-dash-table">
          <thead>
            <tr>
              <th>Club</th>
              <th>Room</th>
              <th>Time</th>
              <th>Purpose</th>
              <th>Comments</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {bookings.map(booking => {
              const bookingKey = `${booking.block}-${booking.roomNo}-${booking.startTime}`;
              return (
                <tr key={bookingKey}>
                  <td>{booking.clubName}</td>
                  <td>{booking.block} {booking.roomNo}</td>
                  <td>{new Date(booking.startTime).toLocaleString()}</td>
                  <td>{booking.purpose}</td>
                  <td>
                    <input
                      type="text"
                      placeholder="Add comments"
                      value={comments[bookingKey] || ''}
                      onChange={e => handleCommentChange(bookingKey, e.target.value)}
                      className="prof-dash-comment"
                    />
                  </td>
                  <td>
                    <button className="prof-dash-approve" onClick={() => handleApproval(booking, 'APPROVED')}>Approve</button>
                    <button className="prof-dash-reject" onClick={() => handleApproval(booking, 'REJECTED')}>Reject</button>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </>
  );
};

export default ProfDash;
