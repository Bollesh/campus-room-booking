import { useNavigate } from 'react-router-dom';
import Header from '../components/header';

export default function studentDash(){
    
    const navigate = useNavigate();

    const handleViewBookings = () => {
        navigate('/student-view');
    };

    const handleCreateBooking = () => {
        navigate('/student-book');
    };

    return (
        <>
            <Header />
            <div className="stu-dashboard">
            <h1>Welcome to the Student Dashboard</h1>
            <div className="buttons-container">
                <button className="large-button" onClick={handleViewBookings}>
                View Bookings
                </button>
                <button className="large-button" onClick={handleCreateBooking}>
                Create Booking
                </button>
            </div>
            </div>
        </>
    );
}